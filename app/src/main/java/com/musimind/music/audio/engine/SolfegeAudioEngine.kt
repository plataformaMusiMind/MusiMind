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
    private var shouldPlayPiano = true // Flag to control piano playback
    
    // Coroutine scope
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Octave offset for pitch detection (-1, 0, +1 = -8va, normal, +8va)
    private var octaveOffset = 0
    
    /**
     * Configura o engine para um novo exercício.
     * 
     * @param notes Lista de notas esperadas
     * @param tempo BPM do exercício
     * @param timeSignatureNumerator Ex: 4 para 4/4
     * @param octaveOffset Offset de oitava: -1 = cantar oitava abaixo, +1 = cantar oitava acima
     */
    fun configure(
        notes: List<ExpectedNote>,
        tempo: Double,
        timeSignatureNumerator: Int = 4,
        timeSignatureDenominator: Int = 4,
        octaveOffset: Int = 0
    ) {
        this.octaveOffset = octaveOffset
        expectedNotes = notes
        
        // Cria novo clock
        audioClock = AudioClock.create(
            sampleRate = SAMPLE_RATE,
            tempo = tempo,
            timeSignatureNumerator = timeSignatureNumerator,
            timeSignatureDenominator = timeSignatureDenominator
        )
        
        // IMPORTANTE: Preparar notas com endSample calculado
        expectedNotes = expectedNotes.map { note ->
            val startSample = audioClock.beatToSample(note.startBeat)
            val durationSamples = (note.durationBeats * audioClock.samplesPerBeat).toLong()
            note.copy(
                startSample = startSample,
                durationSamples = durationSamples,
                endSample = startSample + durationSamples
            )
        }
        
        Log.d(TAG, "Configured with ${expectedNotes.size} notes, tempo=$tempo")
        expectedNotes.forEachIndexed { i, note ->
            Log.d(TAG, "  Note $i: midi=${note.midiNote}, startBeat=${note.startBeat}, durationBeats=${note.durationBeats}, endSample=${note.endSample}")
        }
        
        // Configura analysis engine
        analysisEngine.configure(notes, audioClock, octaveOffset)
        
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
        notes.forEachIndexed { index, note ->
            val startSample = audioClock.beatToSample(note.startBeat)
            val durationSamples = (note.durationBeats * audioClock.samplesPerBeat).toLong()
            
            scheduledEvents.add(
                ScheduledAudioEvent.NoteEvent(
                    samplePosition = startSample,
                    durationSamples = durationSamples,
                    midiNote = note.midiNote,
                    noteIndex = index
                )
            )
        }
    }
    
    /**
     * Inicia reprodução melodia.
     * @param playPiano Se true, toca o piano. Se false, só metrônomo (modo solfege)
     */
    fun startPlayback(playPiano: Boolean = true) {
        if (isRunning) return
        
        shouldPlayPiano = playPiano
        isRunning = true
        phase = SolfegePhase.COUNTDOWN
        currentSamplePosition.set(-audioClock.samplesPerMeasure.toLong())
        
        Log.d(TAG, "Starting playback: playPiano=$playPiano, tempo=${audioClock.tempo}")
        
        engineScope.launch {
            runPlaybackLoop()
        }
    }
    
    private suspend fun runPlaybackLoop() = withContext(Dispatchers.Default) {
        val msPerFrame = (BUFFER_SIZE_FRAMES * 1000.0 / SAMPLE_RATE).toLong()
        
        // Set para rastrear eventos já disparados
        val triggeredEvents = mutableSetOf<ScheduledAudioEvent>()
        
        while (isRunning && isActive) {
            val currentSample = currentSamplePosition.get()
            
            // PRIMEIRO: Atualiza fase quando countdown termina (ANTES de processar eventos)
            if (phase == SolfegePhase.COUNTDOWN && currentSample >= 0) {
                // Modo solfege: LISTENING, Modo playback: PLAYING
                val newPhase = if (shouldPlayPiano) SolfegePhase.PLAYING else SolfegePhase.LISTENING
                phase = newPhase
                updatePhase(newPhase, 0)
                Log.d(TAG, "Phase changed to $newPhase at sample $currentSample")
            }
            
            // SEGUNDO: Processa eventos que devem disparar NESTE frame
            for (event in scheduledEvents) {
                // Pula eventos já disparados
                if (event in triggeredEvents) continue
                
                // Dispara quando currentSample >= samplePosition do evento
                if (currentSample >= event.samplePosition) {
                    triggeredEvents.add(event)
                    
                    when (event) {
                        is ScheduledAudioEvent.CountdownEvent -> {
                            Log.d(TAG, "Countdown beat: ${event.countNumber}")
                            midiPlayer.playMetronomeClick(isAccented = event.countNumber == 1)
                            updatePhase(SolfegePhase.COUNTDOWN, event.countNumber)
                        }
                        is ScheduledAudioEvent.MetronomeClick -> {
                            Log.d(TAG, "Metronome beat: ${event.beatNumber}")
                            midiPlayer.playMetronomeClick(isAccented = event.isAccented)
                            updateBeatDisplay(event.beatNumber)
                        }
                        is ScheduledAudioEvent.NoteEvent -> {
                            // Sempre emitir feedback visual
                            updateCurrentNote(event.noteIndex)
                            Log.d(TAG, "Note event: index=${event.noteIndex}, shouldPlayPiano=$shouldPlayPiano, phase=$phase")
                            
                            // Só toca piano se flag estiver habilitada e estamos em PLAYING
                            if (shouldPlayPiano && phase == SolfegePhase.PLAYING) {
                                val durationMs = (event.durationSamples * 1000 / SAMPLE_RATE).toInt()
                                // Apply octave offset so piano plays at the same octave user will sing
                                val adjustedMidiNote = event.midiNote + (octaveOffset * 12)
                                Log.d(TAG, "Playing note: midi=${adjustedMidiNote} (original=${event.midiNote}, offset=$octaveOffset), duration=${durationMs}ms")
                                midiPlayer.playMidiNote(adjustedMidiNote, durationMs = durationMs)
                            }
                        }
                    }
                }
            }
            
            // TERCEIRO: Verifica fim do exercício
            val lastNote = expectedNotes.lastOrNull()
            if (lastNote != null && currentSample > lastNote.endSample) {
                phase = SolfegePhase.COMPLETED
                updatePhase(SolfegePhase.COMPLETED, 0)
                updateCurrentNote(-1)  // Reset highlight
                Log.d(TAG, "Exercise completed!")
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
     * Agora inclui countdown interno, não depende de startPlayback().
     */
    fun startListening(): Boolean {
        if (isRunning) {
            Log.w(TAG, "Engine already running, skipping startListening")
            return false
        }
        
        if (expectedNotes.isEmpty()) {
            Log.e(TAG, "No notes configured, call configure() first")
            return false
        }
        
        // Verifica permissão
        if (!hasRecordPermission()) {
            Log.w(TAG, "Permissão de microfone não concedida")
            return false
        }
        
        // Configura para modo solfege (sem piano)
        shouldPlayPiano = false
        isRunning = true
        phase = SolfegePhase.COUNTDOWN
        
        // Inicia em posição negativa (1 compasso de countdown)
        currentSamplePosition.set(-audioClock.samplesPerMeasure.toLong())
        
        Log.d(TAG, "Starting listening mode: tempo=${audioClock.tempo}, samplesPerBeat=${audioClock.samplesPerBeat}")
        
        // Inicia loop de playback (countdown + metrônomo)
        engineScope.launch {
            runPlaybackLoop()
        }
        
        // Inicia gravação em paralelo
        return startRecordingInternal()
    }
    
    /**
     * Inicia gravação do microfone (interno).
     */
    private fun startRecordingInternal(): Boolean {
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
                // Get current position from the playback loop clock (don't advance it here!)
                val samplePos = currentSamplePosition.get()
                ringBuffer.write(buffer.copyOf(read), samplePos)
                // NOTE: Do NOT advance currentSamplePosition here!
                // The playback loop is the single source of truth for timing.
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
        updatePhase(SolfegePhase.IDLE, 0)
        updateCurrentNote(-1)  // Reset highlight to initial state
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
    
    private fun updateCurrentNote(noteIndex: Int) {
        _feedbackState.value = _feedbackState.value.copy(
            currentNoteIndex = noteIndex
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
