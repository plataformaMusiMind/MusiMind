package com.musimind.music.audio.scoring

import com.musimind.music.audio.core.*
import kotlin.math.abs

/**
 * Motor de scoring para análise de timing.
 * Avalia a precisão temporal do usuário (quando e por quanto tempo cantou).
 */
object TimingScorer {
    
    // Tolerância em milissegundos
    private const val TOLERANCE_MS_GOOD = 50f    // ±50ms = bom
    private const val TOLERANCE_MS_OK = 100f     // ±100ms = aceitável
    private const val TOLERANCE_MS_MAX = 200f    // ±200ms = máximo
    
    /**
     * Calcula score de timing para uma nota.
     * 
     * @param detectedOnsetSample Sample position do onset detectado (null se não cantou)
     * @param detectedOffsetSample Sample position do offset detectado (null se ainda cantando)
     * @param expectedStartSample Sample position esperado para início
     * @param expectedEndSample Sample position esperado para fim
     * @param sampleRate Taxa de amostragem
     * @return Resultado do scoring
     */
    fun score(
        detectedOnsetSample: Long?,
        detectedOffsetSample: Long?,
        expectedStartSample: Long,
        expectedEndSample: Long,
        sampleRate: Int
    ): TimingScoreResult {
        val toleranceSamples = (TOLERANCE_MS_GOOD * sampleRate / 1000).toLong()
        val maxToleranceSamples = (TOLERANCE_MS_MAX * sampleRate / 1000).toLong()
        
        if (detectedOnsetSample == null) {
            return TimingScoreResult(
                score = 0f,
                status = TimingStatus.NOT_PLAYED,
                attackDeviationSamples = 0,
                attackDeviationMs = 0f,
                durationAccuracy = 0f,
                attackScore = 0f,
                durationScore = 0f
            )
        }
        
        // === SCORE DE ATAQUE (0-50 pontos) ===
        val attackDeviation = detectedOnsetSample - expectedStartSample
        val attackDeviationMs = attackDeviation * 1000f / sampleRate
        
        val attackScore = calculateAttackScore(attackDeviation, toleranceSamples, maxToleranceSamples)
        
        // === SCORE DE DURAÇÃO (0-50 pontos) ===
        val durationScore: Float
        val durationAccuracy: Float
        
        if (detectedOffsetSample != null) {
            val actualDuration = detectedOffsetSample - detectedOnsetSample
            val expectedDuration = expectedEndSample - expectedStartSample
            
            durationAccuracy = (actualDuration.toFloat() / expectedDuration).coerceIn(0f, 2f)
            durationScore = calculateDurationScore(actualDuration, expectedDuration)
        } else {
            // Ainda cantando - avaliação parcial
            durationAccuracy = 0f
            durationScore = 25f // Score parcial
        }
        
        val totalScore = attackScore + durationScore
        
        val status = when {
            abs(attackDeviation) <= toleranceSamples -> TimingStatus.ON_TIME
            attackDeviation < -toleranceSamples -> TimingStatus.EARLY
            else -> TimingStatus.LATE
        }
        
        return TimingScoreResult(
            score = totalScore,
            status = status,
            attackDeviationSamples = attackDeviation,
            attackDeviationMs = attackDeviationMs,
            durationAccuracy = durationAccuracy,
            attackScore = attackScore,
            durationScore = durationScore
        )
    }
    
    private fun calculateAttackScore(
        deviation: Long, 
        tolerance: Long,
        maxTolerance: Long
    ): Float {
        val absDeviation = abs(deviation)
        
        return when {
            absDeviation <= tolerance -> 50f  // Perfeito
            absDeviation <= maxTolerance -> {
                // Interpolação linear
                val ratio = (absDeviation - tolerance).toFloat() / (maxTolerance - tolerance)
                50f * (1f - ratio)
            }
            else -> 0f  // Muito fora
        }
    }
    
    private fun calculateDurationScore(actualDuration: Long, expectedDuration: Long): Float {
        val ratio = actualDuration.toFloat() / expectedDuration
        
        return when {
            ratio in 0.9f..1.1f -> 50f      // Excelente
            ratio in 0.8f..0.9f || ratio in 1.1f..1.2f -> 40f
            ratio in 0.7f..0.8f || ratio in 1.2f..1.3f -> 30f
            ratio in 0.5f..0.7f || ratio in 1.3f..1.5f -> 20f
            ratio in 0.3f..0.5f || ratio in 1.5f..2.0f -> 10f
            else -> 0f
        }
    }
}

/**
 * Resultado do scoring de timing.
 */
data class TimingScoreResult(
    val score: Float,              // 0-100
    val status: TimingStatus,      // ON_TIME, EARLY, LATE, NOT_PLAYED
    val attackDeviationSamples: Long,
    val attackDeviationMs: Float,
    val durationAccuracy: Float,   // Razão duração real/esperada
    val attackScore: Float,        // 0-50
    val durationScore: Float       // 0-50
)
