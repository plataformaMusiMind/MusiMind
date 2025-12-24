package com.musimind.music.audio.core

/**
 * Ring Buffer Single-Producer Single-Consumer (SPSC).
 * Lock-free para uso entre thread de áudio e thread de análise.
 * 
 * INVARIANTES:
 * - Apenas a thread de áudio ESCREVE
 * - Apenas a thread de análise LÊ
 * - Nenhum lock, nenhum synchronized
 * 
 * Usa operações voláteis para visibilidade entre threads.
 */
class SPSCRingBuffer(private val capacity: Int) {
    
    private val buffer = FloatArray(capacity)
    
    // Posições atômicas (volatile para visibilidade)
    @Volatile private var writeIndex = 0L
    @Volatile private var readIndex = 0L
    
    // Sample position correspondente a cada amostra
    private val samplePositions = LongArray(capacity)
    
    /**
     * Escreve samples no buffer (THREAD DE ÁUDIO).
     * Não bloqueia - descarta samples antigos se cheio.
     * 
     * @param samples Array de samples a escrever
     * @param startSamplePosition Posição em samples do primeiro sample
     * @return Número de samples escritos
     */
    fun write(samples: FloatArray, startSamplePosition: Long): Int {
        var written = 0
        var samplePos = startSamplePosition
        
        for (sample in samples) {
            val currentWrite = writeIndex
            val nextWrite = currentWrite + 1
            
            // Verifica se buffer está cheio
            if (nextWrite - readIndex > capacity) {
                // Buffer cheio - incrementa read para descartar amostra antiga
                readIndex++
            }
            
            val index = (currentWrite % capacity).toInt()
            buffer[index] = sample
            samplePositions[index] = samplePos
            
            writeIndex = nextWrite
            written++
            samplePos++
        }
        
        return written
    }
    
    /**
     * Lê samples do buffer (THREAD DE ANÁLISE).
     * 
     * @param output Buffer para samples lidos
     * @param samplePosOutput Buffer para posições em samples
     * @return Número de samples lidos
     */
    fun read(output: FloatArray, samplePosOutput: LongArray): Int {
        var count = 0
        val maxRead = minOf(output.size, (writeIndex - readIndex).toInt())
        
        while (count < maxRead) {
            val currentRead = readIndex
            if (currentRead >= writeIndex) break
            
            val index = (currentRead % capacity).toInt()
            output[count] = buffer[index]
            samplePosOutput[count] = samplePositions[index]
            
            readIndex = currentRead + 1
            count++
        }
        
        return count
    }
    
    /**
     * Lê samples sem atualizar o índice de leitura (peek).
     * Útil para overlap em análise de pitch.
     */
    fun peek(output: FloatArray, offset: Int = 0): Int {
        val available = (writeIndex - readIndex).toInt()
        val startRead = readIndex + offset
        val maxRead = minOf(output.size, available - offset)
        
        if (maxRead <= 0) return 0
        
        for (i in 0 until maxRead) {
            val index = ((startRead + i) % capacity).toInt()
            output[i] = buffer[index]
        }
        
        return maxRead
    }
    
    /**
     * Retorna o sample position do primeiro sample disponível para leitura.
     */
    fun peekSamplePosition(): Long {
        val currentRead = readIndex
        if (currentRead >= writeIndex) return -1
        val index = (currentRead % capacity).toInt()
        return samplePositions[index]
    }
    
    /** Samples disponíveis para leitura */
    fun available(): Int = (writeIndex - readIndex).toInt()
    
    /** Limpa o buffer */
    fun clear() {
        readIndex = writeIndex
    }
    
    /** Capacidade total do buffer */
    fun capacity(): Int = capacity
}
