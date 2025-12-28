package com.musimind.data.network

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Network Retry Manager for Supabase Operations
 * 
 * Implements:
 * - Exponential backoff with jitter
 * - Configurable retry policies
 * - Circuit breaker pattern
 * - Operation-specific retry strategies
 */

@Singleton
class NetworkRetryManager @Inject constructor() {
    
    companion object {
        // Default retry configuration
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_INITIAL_DELAY_MS = 500L
        const val DEFAULT_MAX_DELAY_MS = 30_000L
        const val DEFAULT_BACKOFF_MULTIPLIER = 2.0
        
        // Circuit breaker thresholds
        const val CIRCUIT_BREAKER_THRESHOLD = 5
        const val CIRCUIT_BREAKER_RESET_TIME_MS = 60_000L
    }
    
    // Circuit breaker state
    private var consecutiveFailures = 0
    private var lastFailureTime = 0L
    private var circuitBreakerOpen = false
    
    /**
     * Execute an operation with retry logic
     */
    suspend fun <T> withRetry(
        policy: RetryPolicy = RetryPolicy.DEFAULT,
        operation: suspend () -> T
    ): Result<T> {
        return withRetry(
            maxRetries = policy.maxRetries,
            initialDelayMs = policy.initialDelayMs,
            maxDelayMs = policy.maxDelayMs,
            backoffMultiplier = policy.backoffMultiplier,
            retryCondition = policy.retryCondition,
            operation = operation
        )
    }
    
    /**
     * Execute an operation with configurable retry parameters
     */
    suspend fun <T> withRetry(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
        maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
        backoffMultiplier: Double = DEFAULT_BACKOFF_MULTIPLIER,
        retryCondition: (Exception) -> Boolean = { true },
        operation: suspend () -> T
    ): Result<T> {
        // Check circuit breaker
        if (isCircuitBreakerOpen()) {
            return Result.failure(CircuitBreakerOpenException("Circuit breaker is open. Too many consecutive failures."))
        }
        
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                val result = operation()
                // Success - reset circuit breaker
                onSuccess()
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                
                // Check if we should retry
                if (!retryCondition(e)) {
                    onFailure()
                    return Result.failure(e)
                }
                
                // Last attempt - don't retry
                if (attempt >= maxRetries) {
                    onFailure()
                    return Result.failure(e)
                }
                
                // Calculate delay with jitter
                val delayWithJitter = currentDelay + (Math.random() * currentDelay * 0.1).toLong()
                delay(delayWithJitter)
                
                // Update delay for next retry (exponential backoff)
                currentDelay = min((currentDelay * backoffMultiplier).toLong(), maxDelayMs)
            }
        }
        
        onFailure()
        return Result.failure(lastException ?: Exception("Unknown error"))
    }
    
    /**
     * Execute without retry (single attempt with circuit breaker check)
     */
    suspend fun <T> withCircuitBreaker(operation: suspend () -> T): Result<T> {
        if (isCircuitBreakerOpen()) {
            return Result.failure(CircuitBreakerOpenException("Service temporarily unavailable"))
        }
        
        return try {
            val result = operation()
            onSuccess()
            Result.success(result)
        } catch (e: Exception) {
            onFailure()
            Result.failure(e)
        }
    }
    
    /**
     * Check if circuit breaker is open
     */
    private fun isCircuitBreakerOpen(): Boolean {
        if (!circuitBreakerOpen) return false
        
        // Check if reset time has passed
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFailureTime > CIRCUIT_BREAKER_RESET_TIME_MS) {
            // Reset circuit breaker (half-open state)
            circuitBreakerOpen = false
            consecutiveFailures = CIRCUIT_BREAKER_THRESHOLD / 2
            return false
        }
        
        return true
    }
    
    private fun onSuccess() {
        consecutiveFailures = 0
        circuitBreakerOpen = false
    }
    
    private fun onFailure() {
        consecutiveFailures++
        lastFailureTime = System.currentTimeMillis()
        
        if (consecutiveFailures >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitBreakerOpen = true
        }
    }
    
    /**
     * Reset circuit breaker manually
     */
    fun resetCircuitBreaker() {
        consecutiveFailures = 0
        circuitBreakerOpen = false
    }
    
    /**
     * Get current circuit breaker status
     */
    fun getCircuitBreakerStatus(): CircuitBreakerStatus {
        return CircuitBreakerStatus(
            isOpen = circuitBreakerOpen,
            consecutiveFailures = consecutiveFailures,
            willResetAt = if (circuitBreakerOpen) lastFailureTime + CIRCUIT_BREAKER_RESET_TIME_MS else null
        )
    }
}

/**
 * Predefined retry policies for different operation types
 */
sealed class RetryPolicy(
    val maxRetries: Int,
    val initialDelayMs: Long,
    val maxDelayMs: Long,
    val backoffMultiplier: Double,
    val retryCondition: (Exception) -> Boolean
) {
    /**
     * Default policy - balanced retry behavior
     */
    object DEFAULT : RetryPolicy(
        maxRetries = 3,
        initialDelayMs = 500L,
        maxDelayMs = 10_000L,
        backoffMultiplier = 2.0,
        retryCondition = { isRetryableException(it) }
    )
    
    /**
     * Aggressive retry - more attempts for critical operations
     */
    object AGGRESSIVE : RetryPolicy(
        maxRetries = 5,
        initialDelayMs = 200L,
        maxDelayMs = 30_000L,
        backoffMultiplier = 1.5,
        retryCondition = { isRetryableException(it) }
    )
    
    /**
     * Light retry - quick fail for non-critical operations
     */
    object LIGHT : RetryPolicy(
        maxRetries = 1,
        initialDelayMs = 300L,
        maxDelayMs = 1_000L,
        backoffMultiplier = 2.0,
        retryCondition = { isRetryableException(it) }
    )
    
    /**
     * No retry - single attempt only
     */
    object NO_RETRY : RetryPolicy(
        maxRetries = 0,
        initialDelayMs = 0L,
        maxDelayMs = 0L,
        backoffMultiplier = 1.0,
        retryCondition = { false }
    )
    
    /**
     * Custom policy builder
     */
    class Custom(
        maxRetries: Int,
        initialDelayMs: Long = 500L,
        maxDelayMs: Long = 10_000L,
        backoffMultiplier: Double = 2.0,
        retryCondition: (Exception) -> Boolean = { isRetryableException(it) }
    ) : RetryPolicy(maxRetries, initialDelayMs, maxDelayMs, backoffMultiplier, retryCondition)
    
    companion object {
        /**
         * Determine if an exception is retryable
         */
        fun isRetryableException(e: Exception): Boolean {
            return when {
                // Network errors are retryable
                e is java.net.SocketTimeoutException -> true
                e is java.net.ConnectException -> true
                e is java.net.UnknownHostException -> true
                e is java.io.IOException -> true
                
                // HTTP 5xx errors are retryable
                e.message?.contains("500") == true -> true
                e.message?.contains("502") == true -> true
                e.message?.contains("503") == true -> true
                e.message?.contains("504") == true -> true
                
                // Rate limiting is retryable with backoff
                e.message?.contains("429") == true -> true
                
                // HTTP 4xx client errors are NOT retryable
                e.message?.contains("400") == true -> false
                e.message?.contains("401") == true -> false
                e.message?.contains("403") == true -> false
                e.message?.contains("404") == true -> false
                
                // Default: retry unknown errors once
                else -> true
            }
        }
    }
}

/**
 * Circuit breaker status data class
 */
data class CircuitBreakerStatus(
    val isOpen: Boolean,
    val consecutiveFailures: Int,
    val willResetAt: Long?
)

/**
 * Exception thrown when circuit breaker is open
 */
class CircuitBreakerOpenException(message: String) : Exception(message)

/**
 * Extension function to make retry calls more convenient
 */
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 500L,
    maxDelay: Long = 10_000L,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            // Log the retry
        }
        delay(currentDelay)
        currentDelay = min((currentDelay * factor).toLong(), maxDelay)
    }
    return block() // Last attempt
}

/**
 * Result wrapper extensions
 */
inline fun <T> Result<T>.getOrDefault(default: T): T {
    return getOrNull() ?: default
}

inline fun <T> Result<T>.onSuccessOrError(
    onSuccess: (T) -> Unit,
    onError: (Throwable) -> Unit
) {
    fold(onSuccess) { onError(it) }
}
