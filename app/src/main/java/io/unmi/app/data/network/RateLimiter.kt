package io.unmi.app.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Singleton

/**
 * Token-bucket rate limiter.
 * @param maxTokens max burst capacity
 * @param refillIntervalMs time in ms to refill one token
 */
@Singleton
class RateLimiter(
    private val maxTokens: Int = 5,
    private val refillIntervalMs: Long = 2000L
) {
    private val mutex = Mutex()
    private var availableTokens: Int = maxTokens
    private var lastRefillTime: Long = System.currentTimeMillis()

    suspend fun acquire() {
        mutex.withLock {
            refill()
            if (availableTokens > 0) {
                availableTokens--
                return
            }
        }
        // No tokens, wait for refill
        delay(refillIntervalMs)
        mutex.withLock {
            refill()
            if (availableTokens > 0) {
                availableTokens--
            }
        }
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefillTime
        val tokensToAdd = (elapsed / refillIntervalMs).toInt()
        if (tokensToAdd > 0) {
            availableTokens = (availableTokens + tokensToAdd).coerceAtMost(maxTokens)
            lastRefillTime = now
        }
    }
}
