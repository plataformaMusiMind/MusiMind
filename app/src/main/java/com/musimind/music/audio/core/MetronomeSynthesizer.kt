package com.musimind.music.audio.core

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Geração procedural do click do metrônomo.
 * Não usa samples pré-gravados para evitar latência de I/O.
 * 
 * Gera onda senoidal com envelope exponencial.
 */
object MetronomeSynthesizer {
    
    private const val DEFAULT_SAMPLE_RATE = 44100
    private const val CLICK_DURATION_MS = 15
    
    // Frequências
    private const val ACCENT_FREQ = 1500.0  // Hz (nota acentuada - tempo 1)
    private const val NORMAL_FREQ = 1000.0  // Hz (nota normal)
    
    // Cache de clicks pré-gerados
    private var cachedAccentClick: FloatArray? = null
    private var cachedNormalClick: FloatArray? = null
    private var cachedSampleRate: Int = 0
    
    /**
     * Gera buffer de click do metrônomo.
     * Onda senoidal com envelope exponencial.
     * 
     * @param isAccented true para tempo forte (1), false para outros
     * @param sampleRate Taxa de amostragem
     * @return Buffer com samples do click
     */
    fun generateClick(isAccented: Boolean, sampleRate: Int = DEFAULT_SAMPLE_RATE): FloatArray {
        // Retorna cache se disponível
        if (sampleRate == cachedSampleRate) {
            if (isAccented && cachedAccentClick != null) return cachedAccentClick!!
            if (!isAccented && cachedNormalClick != null) return cachedNormalClick!!
        }
        
        val clickSamples = (sampleRate * CLICK_DURATION_MS / 1000)
        val buffer = FloatArray(clickSamples)
        val frequency = if (isAccented) ACCENT_FREQ else NORMAL_FREQ
        val amplitude = if (isAccented) 0.9f else 0.7f
        
        for (i in 0 until clickSamples) {
            val t = i.toDouble() / sampleRate
            
            // Onda senoidal
            val wave = sin(2.0 * PI * frequency * t)
            
            // Envelope exponencial (decay rápido)
            val envelope = exp(-t * 400.0) // Decay rate
            
            buffer[i] = (wave * envelope * amplitude).toFloat()
        }
        
        // Atualiza cache
        cachedSampleRate = sampleRate
        if (isAccented) {
            cachedAccentClick = buffer
        } else {
            cachedNormalClick = buffer
        }
        
        return buffer
    }
    
    /**
     * Gera buffer de countdown (contagem antes do exercício).
     * Som mais longo e distinto do click normal.
     */
    fun generateCountdownBeep(countNumber: Int, sampleRate: Int = DEFAULT_SAMPLE_RATE): FloatArray {
        val durationMs = 100 // Mais longo que click normal
        val samples = (sampleRate * durationMs / 1000)
        val buffer = FloatArray(samples)
        
        // Frequência mais baixa para countdown
        val baseFreq = 800.0
        val frequency = if (countNumber == 1) baseFreq * 1.5 else baseFreq // Tom mais alto no 1
        
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            
            val wave = sin(2.0 * PI * frequency * t)
            
            // Envelope com attack e decay
            val attackTime = 0.01
            val attack = if (t < attackTime) t / attackTime else 1.0
            val decay = exp(-t * 20.0)
            val envelope = attack * decay
            
            buffer[i] = (wave * envelope * 0.8).toFloat()
        }
        
        return buffer
    }
    
    /**
     * Limpa cache de clicks.
     */
    fun clearCache() {
        cachedAccentClick = null
        cachedNormalClick = null
        cachedSampleRate = 0
    }
}
