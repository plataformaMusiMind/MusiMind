package com.musimind.music.audio.engine

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.musimind.music.audio.core.*
import com.musimind.music.audio.midi.MidiPlayer
import com.musimind.music.audio.scoring.AnalysisEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Engine de áudio principal para exercício de Solfejo.
 * 
 * Integra:
 * - Clock de áudio sample-accurate
 * - Playback (metrônomo + notas)
 * - Captura de microfone
 * - Análise de pitch
 * - Feedback em tempo real
 * 
 * PRINCÍPIOS:
 * - Todo timing é baseado em sample position
 * - Nenhum System.currentTimeMillis()
 * - Comunicação lock-free entre threads
 */
class SolfegeAudioEngine(
    private val context: Context,
    private val midiPlayer: MidiPlayer
) {
    companion object {
        private const val TAG = "SolfegeAudioEngine"
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE_FRAMES = 512 // ~11.6ms
    }
    
    // Estado público
    private val _feedbackState = MutableStateFlow(SolfegeFeedbackState())
    val feedbackState: StateFlow<SolfegeFeedbackState> = _feedbackState.asStateFlow()
    
    // Clock de áudio (sample-accurate)
    private var audioClock = AudioClock.create(sampleRate = SAMPLE_RATE)
    private val currentSamplePosition = AtomicLong(0)
    
    // Ring buffer para comunicação audio → analysis
    private val ringBuffer = SPSCRingBuffer(SAMPLE_RATE * 2) // 2 segundos de buffer
    
    // Analysis engine
    private val analysisEngine = AnalysisEngine(SAMPLE_RATE) { state ->
        _feedbackState.value = state
    }
    
    // Audio recording
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var analysisJob: Job? = null
    
    // Notas do exercício
    private var expectedNotes: List<ExpectedNote> = emptyList()
    
    // Eventos agendados
    private val scheduledEvents = mutableListOf<ScheduledAudioEvent>()
    
    // Estado
    private var isRunning = false
    private var phase = SolfegePhase.IDLE
    
    // Coroutine scope
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Configura o engine para um novo exercício.
     * 
     * @param notes Lista de notas esperadas
     * @param tempo BPM do exercício
     * @param timeSignatureNumerator Ex: 4 para 4/4
     */
    fun configure(
        notes: List<ExpectedNote>,
        tempo: Double,
        timeSignatureNumerator: Int = 4,
        timeSignatureDenominator: Int = 4
    ) {
        expectedNotes = notes
        
        // Cria novo clock
        audioClock = AudioClock.create(
            sampleRate = SAMPLE_RATE,
            tempo = tempo,
            timeSignatureNumerator = timeSignatureNumerator,
            timeSignatureDenominator = timeSignatureDenominator
        )
        
        // Configura analysis engine
        analysisEngine.configure(notes, audioClock)
        
        // Limpa eventos anteriores
        scheduledEvents.clear()
        
        // Agenda eventos de countdown (1 compasso antes)
        scheduleCountdown()
        
        // Agenda eventos de metrônomo
        scheduleMetronome(notes)
        
        // Agenda notas para playback
        scheduleNotes(notes)
        
        phase = SolfegePhase.IDLE
        currentSamplePosition.set(0)
    }
    
    private fun scheduleCountdown() {
        // 4 beats de countdown antes do início
        for (beat in 0 until audioClock.timeSignatureNumerator) {
            val samplePos = -(audioClock.timeSignatureNumerator - beat).toLong() * 
                            audioClock.samplesPerBeat.toLong()
            
            scheduledEvents.add(
                ScheduledAudioEvent.CountdownEvent(
                    samplePosition = samplePos,
                    countNumber = beat + 1
                )
            )
        }
    }
    
    private fun scheduleMetronome(notes: List<ExpectedNote>) {
        if (notes.isEmpty()) return
        
        // Calcula duração total em beats
        val totalBeats = notes.maxOfOrNull { it.startBeat + it.durationBeats } ?: 0.0
        val totalMeasures = (totalBeats / audioClock.timeSignatureNumerator).toInt() + 1
        
        // Agenda um click por beat
        for (measure in 0 until totalMeasures) {
            for (beat in 0 until audioClock.timeSignatureNumerator) {
                val beatPosition = measure * audioClock.timeSignatureNumerator + beat
                val samplePos = audioClock.beatToSample(beatPosition.toDouble())
                
                scheduledEvents.add(
                    ScheduledAudioEvent.MetronomeClick(
                        samplePosition = samplePos,
                        isAccented = beat == 0,
                        beatNumber = beat + 1
                    )
                )
            }
        }
    }
    
    private fun scheduleNotes(notes: List<ExpectedNote>) {
        for (note in notes) {
            val startSample = audioClock.beatToSample(note.startBeat)
            val durationSamples = (note.durationBeats * audioClock.samplesPerBeat).toLong()
            
            scheduledEvents.add(
                ScheduledAudioEvent.NoteEvent(
                    samplePosition = startSample,
                    durationSamples = durationSamples,
                    midiNote = note.midiNote
                )
            )
        }
    }
    
    /**
     * Inicia reprodução melodia (usuário ouve).
     */
    fun startPlayback() {
        if (isRunning) return
        
        isRunning = true
        phase = SolfegePhase.COUNTDOWN
        currentSamplePosition.set(-audioClock.samplesPerMeasure.toLong())
        
        engineScope.launch {
            runPlaybackLoop()
        }
    }
    
    private suspend fun runPlaybackLoop() = withContext(Dispatchers.Default) {
        val msPerFrame = (BUFFER_SIZE_FRAMES * 1000.0 / SAMPLE_RATE).toLong()
        
        while (isRunning && isActive) {
            val frameStart = currentSamplePosition.get()
            val frameEnd = frameStart + BUFFER_SIZE_FRAMES
            
            // Processa eventos que caem neste frame
            val eventsInFrame = scheduledEvents.filter { event ->
                event.isInFrame(frameStart, frameEnd)
            }
            
            for (event in eventsInFrame) {
                when (event) {
                    is ScheduledAudioEvent.CountdownEvent -> {
                        midiPlayer.playMetronomeClick(isAccented = event.countNumber == 1)
                        updatePhase(SolfegePhase.COUNTDOWN, event.countNumber)
                    }
                    is ScheduledAudioEvent.MetronomeClick -> {
                        if (frameStart >= 0) {
                            midiPlayer.playMetronomeClick(isAccented = event.isAccented)
                            updateBeatDisplay(event.beatNumber)
                        }
                    }
                    is ScheduledAudioEvent.NoteEvent -> {
                        if (phase == SolfegePhase.PLAYING) {
                            val durationMs = (event.durationSamples * 1000 / SAMPLE_RATE).toInt()
                            midiPlayer.playPitch(event.midiNote, durationMs = (durationMs * 0.95).toInt())
                        }
                    }
                }
            }
            
            // Atualiza fase quando countdown termina
            if (phase == SolfegePhase.COUNTDOWN && frameStart >= 0) {
                phase = SolfegePhase.PLAYING
                updatePhase(SolfegePhase.PLAYING, 0)
            }
            
            // Verifica fim do exercício
            val lastNote = expectedNotes.lastOrNull()
            if (lastNote != null && frameStart > lastNote.endSample) {
                phase = SolfegePhase.COMPLETED
                updatePhase(SolfegePhase.COMPLETED, 0)
                isRunning = false
                break
            }
            
            // Avança clock
            currentSamplePosition.addAndGet(BUFFER_SIZE_FRAMES.toLong())
            
            delay(msPerFrame)
        }
    }
    
    /**
     * Inicia modo de escuta (usuário canta).
     */
    fun startListening(): Boolean {
        if (recordingJob?.isActive == true) return false
        
        // Verifica permissão
        if (!hasRecordPermission()) {
            Log.w(TAG, "Permissão de microfone não concedida")
            return false
        }
        
        // Inicia gravação
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(BUFFER_SIZE_FRAMES * 4)
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Falha ao inicializar AudioRecord")
                return false
            }
            
            audioRecord?.startRecording()
            phase = SolfegePhase.LISTENING
            currentSamplePosition.set(0)
            isRunning = true
            
            // Inicia thread de gravação
            recordingJob = engineScope.launch(Dispatchers.IO) {
                runRecordingLoop()
            }
            
            // Inicia thread de análise
            analysisJob = engineScope.launch(Dispatchers.Default) {
                runAnalysisLoop()
            }
            
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Erro de permissão: ${e.message}")
            return false
        }
    }
    
    private suspend fun runRecordingLoop() = withContext(Dispatchers.IO) {
        val buffer = FloatArray(BUFFER_SIZE_FRAMES)
        
        while (isRunning && isActive) {
            val read = audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: 0
            
            if (read > 0) {
                val samplePos = currentSamplePosition.get()
                ringBuffer.write(buffer.copyOf(read), samplePos)
                currentSamplePosition.addAndGet(read.toLong())
            }
        }
    }
    
    private suspend fun runAnalysisLoop() = withContext(Dispatchers.Default) {
        val analysisBuffer = FloatArray(2048) // Window size do YIN
        val samplePosBuffer = LongArray(2048)
        
        while (isRunning && isActive) {
            // Espera ter samples suficientes
            if (ringBuffer.available() >= 2048) {
                val read = ringBuffer.read(analysisBuffer, samplePosBuffer)
                
                if (read >= 2048) {
                    val samplePos = samplePosBuffer[0]
                    analysisEngine.process(analysisBuffer, samplePos)
                }
            }
            
            delay(10) // ~100 Hz de análise
        }
    }
    
    /**
     * Para reprodução/gravação.
     */
    fun stop() {
        isRunning = false
        recordingJob?.cancel()
        analysisJob?.cancel()
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        phase = SolfegePhase.IDLE
    }
    
    /**
     * Limpa recursos.
     */
    fun release() {
        stop()
        engineScope.cancel()
    }
    
    private fun updatePhase(newPhase: SolfegePhase, countdownNumber: Int) {
        _feedbackState.value = _feedbackState.value.copy(
            phase = newPhase,
            countdownNumber = countdownNumber
        )
        analysisEngine.setPhase(newPhase)
    }
    
    private fun updateBeatDisplay(beatNumber: Int) {
        _feedbackState.value = _feedbackState.value.copy(
            currentBeatInMeasure = beatNumber,
            isDownbeat = beatNumber == 1
        )
    }
    
    private fun hasRecordPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
               android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Retorna clock atual.
     */
    fun getAudioClock(): AudioClock = audioClock.copy(samplePosition = currentSamplePosition.get())
    
    /**
     * Retorna fase atual.
     */
    fun getCurrentPhase(): SolfegePhase = phase
}
