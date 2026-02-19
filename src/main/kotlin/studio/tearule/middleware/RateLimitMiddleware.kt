package studio.tearule.middleware

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.origin
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Simple token bucket implementation for rate limiting
 */
class TokenBucket(
    private val capacity: Double = 100.0, // Maximum tokens
    private val refillRate: Double = 10.0  // Tokens per second
) {
    private var tokens: Double = capacity
    private var lastRefillTime: Long = System.currentTimeMillis()
    private val mutex = Mutex()

    suspend fun tryConsume(tokensToConsume: Double = 1.0): Boolean {
        return mutex.withLock {
            refill()
            if (tokens >= tokensToConsume) {
                tokens -= tokensToConsume
                true
            } else {
                false
            }
        }
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val timePassed = (now - lastRefillTime) / 1000.0 // seconds
        val tokensToAdd = timePassed * refillRate
        tokens = minOf(capacity, tokens + tokensToAdd)
        lastRefillTime = now
    }

    fun getRemainingTokens(): Double = tokens
}

/**
 * Rate limiting middleware using token bucket algorithm
 */
class RateLimitMiddleware(
    private val requestsPerMinute: Int = 60,
    private val burstCapacity: Int = 10
) {
    private val buckets = ConcurrentHashMap<String, TokenBucket>()
    private val refillRate = requestsPerMinute / 60.0 // tokens per second
    private val capacity = burstCapacity.toDouble()

    suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val call = context.call
        val clientKey = getClientKey(call)

        val bucket = buckets.computeIfAbsent(clientKey) {
            TokenBucket(capacity, refillRate)
        }

        if (!bucket.tryConsume()) {
            call.respond(HttpStatusCode.TooManyRequests, mapOf(
                "error" to "Rate limit exceeded",
                "retry_after" to 60, // seconds
                "limit" to requestsPerMinute,
                "remaining" to bucket.getRemainingTokens().toInt()
            ))
            context.finish()
            return
        }

        context.proceed()
    }

    private fun getClientKey(call: ApplicationCall): String {
        // Use client IP as key, or could use user ID if authenticated
        return call.request.origin.remoteHost ?: "unknown"
    }
}
