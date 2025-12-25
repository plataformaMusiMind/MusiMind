package com.musimind.music.audio.scoring

import com.musimind.music.audio.core.*
import kotlin.math.abs

/**
 * Motor de scoring para análise de timing.
 * 
 * Avalia a precisão temporal do usuário:
 * 1. ATTACK: Quando a voz começou em relação ao início esperado da nota
 * 2. DURATION: Quanto tempo a voz durou comparado com a duração da figura
 * 
 * Tolerâncias baseadas em performance vocal realista:
 * - Breathing room: tempo para respirar entre notas (~100-150ms)
 * - Human reaction: tempo de reação (~80ms)
 */
object TimingScorer {
    
    // === CONSTANTES PARA CÁLCULO DE TEMPO ===
    // Exemplo: tempo=60 BPM, sampleRate=44100
    // samplesPerBeat = 44100 * 60 / 60 = 44100 samples/beat
    // 1 semínima = 44100 samples = 1000ms
    // 1 colcheia = 22050 samples = 500ms
    
    // === TOLERÂNCIA DE ATAQUE ===
    // Quando o usuário pode começar a cantar em relação ao beat esperado
    private const val ATTACK_TOLERANCE_EARLY_MS = 150f   // Pode começar até 150ms ANTES
    private const val ATTACK_TOLERANCE_LATE_MS = 200f    // Pode começar até 200ms DEPOIS
    private const val ATTACK_MAX_LATE_MS = 400f          // Limite máximo de atraso
    
    // === TOLERÂNCIA DE DURAÇÃO ===
    // Quanto da nota precisa ser sustentada
    private const val BREATHING_TOLERANCE_MS = 180f      // Pode parar 180ms antes para respirar
    private const val MIN_DURATION_PERCENT = 0.55f       // Mínimo 55% da duração da nota
    private const val IDEAL_DURATION_PERCENT = 0.70f     // Ideal: 70% ou mais
    
    /**
     * Calcula score de timing para uma nota.
     * 
     * Fórmulas de conversão:
     * - samplesPerBeat = sampleRate * 60 / tempo
     * - noteDurationSamples = durationBeats * samplesPerBeat
     * - toleranceSamples = toleranceMs * sampleRate / 1000
     * 
     * @param detectedOnsetSample Sample onde a voz começou (null se não cantou)
     * @param detectedOffsetSample Sample onde a voz terminou (null se ainda cantando)
     * @param expectedStartSample Sample esperado para início da nota
     * @param expectedEndSample Sample esperado para fim da nota
     * @param sampleRate Taxa de amostragem (44100 Hz)
     */
    fun score(
        detectedOnsetSample: Long?,
        detectedOffsetSample: Long?,
        expectedStartSample: Long,
        expectedEndSample: Long,
        sampleRate: Int
    ): TimingScoreResult {
        // === CONVERSÕES DE TEMPO ===
        val attackEarlyTolerance = msToSamples(ATTACK_TOLERANCE_EARLY_MS, sampleRate)
        val attackLateTolerance = msToSamples(ATTACK_TOLERANCE_LATE_MS, sampleRate)
        val attackMaxLate = msToSamples(ATTACK_MAX_LATE_MS, sampleRate)
        val breathingTolerance = msToSamples(BREATHING_TOLERANCE_MS, sampleRate)
        
        // Duração esperada da figura em samples e ms
        val expectedDurationSamples = expectedEndSample - expectedStartSample
        val expectedDurationMs = samplesToMs(expectedDurationSamples, sampleRate)
        
        // Se não detectou onset, não cantou a nota
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
        
        // === 1. AVALIAÇÃO DO ATTACK ===
        // Desvio do attack: positivo = atrasou, negativo = adiantou
        val attackDeviation = detectedOnsetSample - expectedStartSample
        val attackDeviationMs = samplesToMs(attackDeviation, sampleRate)
        
        val attackScore = calculateAttackScore(
            attackDeviation,
            attackEarlyTolerance,
            attackLateTolerance,
            attackMaxLate
        )
        
        // === 2. AVALIAÇÃO DA DURAÇÃO ===
        val durationScore: Float
        val durationAccuracy: Float
        
        if (detectedOffsetSample != null) {
            // Duração que o usuário realmente cantou
            val actualDurationSamples = detectedOffsetSample - detectedOnsetSample
            
            // Duração mínima aceitável (considerando respiração)
            val minAcceptableDuration = expectedDurationSamples - breathingTolerance
            val idealDuration = expectedDurationSamples * IDEAL_DURATION_PERCENT
            
            // Razão: quanto da nota foi cantada
            durationAccuracy = (actualDurationSamples.toFloat() / expectedDurationSamples)
                .coerceIn(0f, 2f)
            
            durationScore = calculateDurationScore(
                actualDurationSamples,
                expectedDurationSamples,
                minAcceptableDuration.toLong(),
                idealDuration.toLong()
            )
        } else {
            // Ainda cantando - avaliação parcial positiva
            durationAccuracy = 0.85f
            durationScore = 35f
        }
        
        // Score total (0-100)
        val totalScore = attackScore + durationScore
        
        // Determina status baseado no attack
        val status = when {
            attackDeviation >= -attackEarlyTolerance && attackDeviation <= attackLateTolerance -> 
                TimingStatus.ON_TIME
            attackDeviation < -attackEarlyTolerance -> TimingStatus.EARLY
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
    
    /**
     * Calcula score do attack (0-50 pontos).
     * 
     * - Dentro da tolerância ideal: 50 pontos
     * - Um pouco fora: interpolação linear
     * - Muito fora: pontuação mínima
     */
    private fun calculateAttackScore(
        deviation: Long,
        earlyTolerance: Long,
        lateTolerance: Long,
        maxLate: Long
    ): Float {
        return when {
            // Perfeito: dentro da janela de tolerância
            deviation >= -earlyTolerance && deviation <= lateTolerance -> 50f
            
            // Adiantou um pouco (entre -200ms e -100ms)
            deviation < -earlyTolerance && deviation >= -earlyTolerance * 2 -> {
                // Interpolação de 50 para 35
                val overshoot = abs(deviation) - earlyTolerance
                val range = earlyTolerance
                50f - (overshoot.toFloat() / range) * 15f
            }
            
            // Atrasou um pouco (entre 150ms e 300ms)
            deviation > lateTolerance && deviation <= maxLate -> {
                // Interpolação de 50 para 20
                val overshoot = deviation - lateTolerance
                val range = maxLate - lateTolerance
                50f - (overshoot.toFloat() / range) * 30f
            }
            
            // Muito adiantado ou muito atrasado
            else -> 10f  // Pontuação mínima por tentar
        }
    }
    
    /**
     * Calcula score de duração (0-50 pontos).
     * 
     * - Cantou duração ideal ou mais: 50 pontos
     * - Cantou duração mínima aceitável: 35 pontos
     * - Cantou menos que mínimo: proporção de pontos
     */
    private fun calculateDurationScore(
        actualDuration: Long,
        expectedDuration: Long,
        minDuration: Long,
        idealDuration: Long
    ): Float {
        return when {
            // Excelente: cantou 80% ou mais da nota
            actualDuration >= idealDuration -> 50f
            
            // Bom: cantou entre mínimo (65%) e ideal (80%)
            actualDuration >= minDuration -> {
                // Interpolação de 35 para 50
                val progress = (actualDuration - minDuration).toFloat() / 
                               (idealDuration - minDuration)
                35f + progress * 15f
            }
            
            // Aceitável: cantou entre 50% e 65%
            actualDuration >= expectedDuration * 0.5f -> {
                // Interpolação de 20 para 35
                val minSamples = (expectedDuration * 0.5f).toLong()
                val progress = (actualDuration - minSamples).toFloat() / 
                               (minDuration - minSamples)
                20f + progress * 15f
            }
            
            // Muito curto: staccato involuntário
            actualDuration >= expectedDuration * 0.3f -> 15f
            
            // Quase não cantou
            else -> 5f
        }
    }
    
    // === FUNÇÕES DE CONVERSÃO ===
    
    private fun msToSamples(ms: Float, sampleRate: Int): Long {
        return (ms * sampleRate / 1000f).toLong()
    }
    
    private fun samplesToMs(samples: Long, sampleRate: Int): Float {
        return samples * 1000f / sampleRate
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
