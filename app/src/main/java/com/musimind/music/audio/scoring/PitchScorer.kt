package com.musimind.music.audio.scoring

import com.musimind.music.audio.core.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Motor de scoring para análise de pitch.
 * Avalia a precisão do pitch do usuário em relação à nota esperada.
 * 
 * A nota MIDI deve ser EXATAMENTE correta.
 * Tolerância é apenas para variação em CENTS dentro da mesma nota.
 * Vibrato natural e ruídos são filtrados, não aceitos como corretos.
 */
object PitchScorer {
    
    // Tolerância em cents para considerar "correto" (variação DENTRO da mesma nota)
    // Um semitom = 100 cents, então ±50 cents = meio semitom
    private const val TOLERANCE_CENTS_GOOD = 30f   // ±30 cents = excelente
    private const val TOLERANCE_CENTS_OK = 45f     // ±45 cents = bom
    private const val TOLERANCE_CENTS_MAX = 50f    // ±50 cents = máximo (não permite meio tom de erro)
    
    // Mínimo de frames na nota correta para considerar válido
    // Frames fora são considerados ruído/vibrato e IGNORADOS (não contados como erros)
    private const val MIN_CORRECT_FRAMES_RATIO = 0.40f  // 40% dos frames voiced devem ser na nota correta
    
    // Mínimo de frames para avaliar (evita avaliação de ruído curto)
    private const val MIN_FRAMES_FOR_EVALUATION = 3
    
    /**
     * Calcula score de pitch para uma nota.
     * 
     * A lógica é:
     * 1. Filtra frames voiced
     * 2. Separa frames na nota EXATA esperada vs frames em outras notas
     * 3. Frames em outras notas são considerados ruído/vibrato e IGNORADOS
     * 4. Score é baseado apenas nos frames na nota correta
     * 5. Se menos de 40% dos frames estão na nota correta, a nota está errada
     */
    fun score(
        pitchFrames: List<PitchFrame>,
        expectedMidiNote: Int,
        octaveOffset: Int = 0
    ): PitchScoreResult {
        if (pitchFrames.isEmpty()) {
            return notEvaluated()
        }
        
        // Filtra apenas frames voiced
        val voicedFrames = pitchFrames.filter { it.isVoiced }
        
        if (voicedFrames.size < MIN_FRAMES_FOR_EVALUATION) {
            return notEvaluated()
        }
        
        // Apply octave offset: adjust detected note to compare with expected
        val adjustedFrames = voicedFrames.map { frame ->
            frame.copy(midiNote = frame.midiNote - (octaveOffset * 12))
        }
        
        // Separa frames na nota EXATA esperada
        val correctNoteFrames = adjustedFrames.filter { it.midiNote == expectedMidiNote }
        val correctRatio = correctNoteFrames.size.toFloat() / adjustedFrames.size
        
        // Se poucos frames estão na nota correta, o usuário cantou a nota ERRADA
        if (correctRatio < MIN_CORRECT_FRAMES_RATIO) {
            // Encontra qual nota foi realmente cantada
            val mostCommonNote = adjustedFrames.groupBy { it.midiNote }
                .maxByOrNull { it.value.size }
                ?.key ?: expectedMidiNote
            
            val semitoneError = mostCommonNote - expectedMidiNote
            val status = if (semitoneError < 0) PitchStatus.FLAT else PitchStatus.SHARP
            
            return PitchScoreResult(
                score = 0f,  // Zero score - wrong note
                status = status,
                avgCentDeviation = (semitoneError * 100).toFloat(),
                stdDeviation = 0f,
                correctRatio = correctRatio
            )
        }
        
        // Usuário cantou a nota CORRETA
        // Agora avaliamos a precisão em CENTS apenas dos frames na nota correta
        // (frames em outras notas são ruído/vibrato e ignorados)
        
        val centDeviations = correctNoteFrames.map { it.centDeviation }
        val avgCentDeviation = centDeviations.average().toFloat()
        val stdDeviation = calculateStdDev(centDeviations)
        
        // Score baseado no desvio médio em cents
        val deviationScore = when {
            abs(avgCentDeviation) <= TOLERANCE_CENTS_GOOD -> 100f
            abs(avgCentDeviation) <= TOLERANCE_CENTS_OK -> {
                // Interpolação de 100 para 80
                val ratio = (abs(avgCentDeviation) - TOLERANCE_CENTS_GOOD) / 
                            (TOLERANCE_CENTS_OK - TOLERANCE_CENTS_GOOD)
                100f - (ratio * 20f)
            }
            abs(avgCentDeviation) <= TOLERANCE_CENTS_MAX -> {
                // Interpolação de 80 para 60
                val ratio = (abs(avgCentDeviation) - TOLERANCE_CENTS_OK) / 
                            (TOLERANCE_CENTS_MAX - TOLERANCE_CENTS_OK)
                80f - (ratio * 20f)
            }
            else -> 50f  // Fora da tolerância mas ainda na nota certa
        }
        
        // Pequena penalidade por frames "errados" (ruído/vibrato)
        // Mas não muito, pois é normal ter algum ruído
        val noiseRatio = 1f - correctRatio
        val noisePenalty = noiseRatio * 15f  // Máximo 15% de penalidade por ruído
        
        val score = (deviationScore - noisePenalty).coerceIn(0f, 100f)
        
        // Status baseado no desvio em cents
        val status = when {
            abs(avgCentDeviation) <= TOLERANCE_CENTS_OK -> PitchStatus.CORRECT
            avgCentDeviation < 0 -> PitchStatus.FLAT
            else -> PitchStatus.SHARP
        }
        
        return PitchScoreResult(
            score = score,
            status = status,
            avgCentDeviation = avgCentDeviation,
            stdDeviation = stdDeviation,
            correctRatio = correctRatio
        )
    }
    
    private fun notEvaluated() = PitchScoreResult(
        score = 0f,
        status = PitchStatus.NOT_EVALUATED,
        avgCentDeviation = 0f,
        stdDeviation = 0f,
        correctRatio = 0f
    )
    
    private fun calculateStdDev(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }
}

/**
 * Resultado do scoring de pitch.
 */
data class PitchScoreResult(
    val score: Float,              // 0-100
    val status: PitchStatus,       // CORRECT, FLAT, SHARP
    val avgCentDeviation: Float,   // Média do desvio em cents
    val stdDeviation: Float,       // Desvio padrão (estabilidade)
    val correctRatio: Float        // Proporção de frames na nota correta
)
