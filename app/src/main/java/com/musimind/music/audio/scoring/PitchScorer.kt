package com.musimind.music.audio.scoring

import com.musimind.music.audio.core.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Motor de scoring para análise de pitch.
 * Avalia a precisão do pitch do usuário em relação à nota esperada.
 */
object PitchScorer {
    
    // Tolerância em cents para considerar "correto"
    private const val TOLERANCE_CENTS_GOOD = 20f   // ±20 cents = bom
    private const val TOLERANCE_CENTS_OK = 35f     // ±35 cents = aceitável
    private const val TOLERANCE_CENTS_MAX = 50f    // ±50 cents = máximo (meio tom)
    
    /**
     * Calcula score de pitch para uma nota.
     * 
     * @param pitchFrames Lista de PitchFrames detectados durante a nota
     * @param expectedMidiNote Nota MIDI esperada
     * @return Resultado do scoring
     */
    fun score(
        pitchFrames: List<PitchFrame>,
        expectedMidiNote: Int
    ): PitchScoreResult {
        if (pitchFrames.isEmpty()) {
            return PitchScoreResult(
                score = 0f,
                status = PitchStatus.NOT_EVALUATED,
                avgCentDeviation = 0f,
                stdDeviation = 0f,
                correctRatio = 0f
            )
        }
        
        // Filtra apenas frames voiced com a nota correta (ou próxima)
        val voicedFrames = pitchFrames.filter { it.isVoiced }
        
        if (voicedFrames.isEmpty()) {
            return PitchScoreResult(
                score = 0f,
                status = PitchStatus.NOT_EVALUATED,
                avgCentDeviation = 0f,
                stdDeviation = 0f,
                correctRatio = 0f
            )
        }
        
        // Conta quantos frames estão na nota correta
        val correctNoteFrames = voicedFrames.filter { it.midiNote == expectedMidiNote }
        val correctRatio = correctNoteFrames.size.toFloat() / voicedFrames.size
        
        // Se maioria não está na nota certa, provavelmente cantou nota errada
        if (correctRatio < 0.5f) {
            // Encontra a nota que mais cantou
            val mostCommonNote = voicedFrames.groupBy { it.midiNote }
                .maxByOrNull { it.value.size }
                ?.key ?: expectedMidiNote
            
            val status = if (mostCommonNote < expectedMidiNote) PitchStatus.FLAT else PitchStatus.SHARP
            
            return PitchScoreResult(
                score = correctRatio * 30f, // Score muito baixo
                status = status,
                avgCentDeviation = ((mostCommonNote - expectedMidiNote) * 100).toFloat(),
                stdDeviation = 0f,
                correctRatio = correctRatio
            )
        }
        
        // Calcula estatísticas dos frames na nota correta
        val centDeviations = correctNoteFrames.map { it.centDeviation }
        val avgCentDeviation = centDeviations.average().toFloat()
        val stdDeviation = calculateStdDev(centDeviations)
        
        // Score base (0-100)
        // Penalidade por desvio médio
        val deviationPenalty = (abs(avgCentDeviation) / TOLERANCE_CENTS_MAX).coerceAtMost(1f) * 40f
        
        // Penalidade por instabilidade (vibrato excessivo)
        val instabilityPenalty = (stdDeviation / 30f).coerceAtMost(1f) * 20f
        
        // Penalidade por frames incorretos
        val incorrectPenalty = (1f - correctRatio) * 30f
        
        val score = (100f - deviationPenalty - instabilityPenalty - incorrectPenalty)
            .coerceIn(0f, 100f)
        
        val status = when {
            abs(avgCentDeviation) <= TOLERANCE_CENTS_GOOD -> PitchStatus.CORRECT
            avgCentDeviation < -TOLERANCE_CENTS_GOOD -> PitchStatus.FLAT
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
