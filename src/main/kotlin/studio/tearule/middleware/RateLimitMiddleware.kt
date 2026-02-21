package studio.tearule.middleware

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.PipelineCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import io.ktor.server.plugins.origin
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
        tokens = (tokens + timePassed * refillRate).coerceAtMost(capacity)
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

    suspend fun intercept(context: PipelineContext<Unit, PipelineCall>) {
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

    private fun getClientKey(call: PipelineCall): String {
        // Prefer X-Forwarded-For when behind a proxy; fall back to origin remoteHost
        return call.request.headers["X-Forwarded-For"]?.substringBefore(",")?.trim()
            ?: call.request.origin.remoteHost
    }
}
