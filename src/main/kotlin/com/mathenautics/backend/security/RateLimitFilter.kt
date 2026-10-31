package com.mathenautics.backend.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.mathenautics.backend.dto.ApiError
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory, IP + endpoint rate limiter for authentication-related endpoints.
 *
 * Scope (F-09):
 *   POST /api/v1/auth/login
 *   POST /api/v1/users
 *
 * Any other request (method or path) is passed through untouched.
 *
 * Limitations:
 *   - State is per-instance and resets on process restart.
 *   - Clients behind a shared NAT share a bucket.
 *   - X-Forwarded-For is only trustworthy behind a proxy that overwrites it
 *     (Render does).
 *   - When the bounded bucket store is full, requests for new client/endpoint
 *     keys are rejected until an existing bucket expires. Existing buckets
 *     continue to be enforced normally.
 */
class RateLimitFilter(
    private val properties: RateLimitProperties,
    private val objectMapper: ObjectMapper,
    private val maxBuckets: Int = DEFAULT_MAX_BUCKETS
) : OncePerRequestFilter() {

    companion object {
        const val LOGIN_PATH = "/api/v1/auth/login"
        const val REGISTRATION_PATH = "/api/v1/users"

        private const val X_FORWARDED_FOR = "X-Forwarded-For"
        private const val RETRY_AFTER_HEADER = "Retry-After"
        private const val CLEANUP_INTERVAL_MS = 5L * 60L * 1000L
        const val DEFAULT_MAX_BUCKETS = 10_000
    }

    private enum class Rule(val path: String) {
        LOGIN(RateLimitFilter.LOGIN_PATH),
        REGISTRATION(RateLimitFilter.REGISTRATION_PATH)
    }

    private class Bucket(val expiresAt: Instant) {
        val counter = AtomicInteger(0)
    }

    private val buckets = ConcurrentHashMap<String, Bucket>()
    private val bucketCreationLock = Any()
    private val lastCleanupMs = AtomicLong(System.currentTimeMillis())

    init {
        require(maxBuckets > 0) { "maxBuckets must be greater than zero" }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val rule = ruleFor(request)
        if (rule == null) {
            filterChain.doFilter(request, response)
            return
        }

        maybeCleanup()

        val limit = limitFor(rule)
        val key = "${clientIp(request)}|${rule.name}"
        val now = Instant.now()
        val bucket = getOrCreateBucket(key, now, limit.windowSeconds)

        if (bucket == null) {
            // Capacity exhaustion fails closed for new keys; existing buckets
            // remain usable and keep their original counters.
            val nextExpiry = buckets.values.minOfOrNull { it.expiresAt }
                ?: now.plusSeconds(limit.windowSeconds.toLong())
            writeRateLimited(response, nextExpiry, now)
            return
        }

        val attempt = bucket.counter.incrementAndGet()

        if (attempt > limit.maxRequests) {
            writeRateLimited(response, bucket.expiresAt, now)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun getOrCreateBucket(key: String, now: Instant, windowSeconds: Int): Bucket? {
        val existing = buckets[key]
        if (existing != null && now.isBefore(existing.expiresAt)) {
            return existing
        }

        synchronized(bucketCreationLock) {
            val current = buckets[key]
            if (current != null) {
                if (now.isBefore(current.expiresAt)) {
                    return current
                }
                buckets.remove(key, current)
            }

            // A capacity check always performs a full expiry sweep, so stale
            // entries cannot keep the map full until the periodic cleanup runs.
            val cutoff = Instant.now()
            buckets.entries.removeIf { !cutoff.isBefore(it.value.expiresAt) }

            if (buckets.size >= maxBuckets) {
                return null
            }

            return Bucket(now.plusSeconds(windowSeconds.toLong())).also {
                buckets[key] = it
            }
        }
    }

    private fun ruleFor(request: HttpServletRequest): Rule? {
        if (request.method != "POST") return null
        val uri = request.requestURI ?: return null
        return Rule.entries.firstOrNull { it.path == uri }
    }

    private fun limitFor(rule: Rule): RateLimitProperties.Rule = when (rule) {
        Rule.LOGIN -> properties.login
        Rule.REGISTRATION -> properties.registration
    }

    private fun clientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader(X_FORWARDED_FOR)
        if (!forwarded.isNullOrBlank()) {
            return forwarded.split(",").first().trim()
        }
        return request.remoteAddr ?: "unknown"
    }

    private fun writeRateLimited(
        response: HttpServletResponse,
        expiresAt: Instant,
        now: Instant
    ) {
        val retryAfter = Duration.between(now, expiresAt).seconds.coerceAtLeast(1L)

        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader(RETRY_AFTER_HEADER, retryAfter.toString())
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            objectMapper.writeValueAsString(
                ApiError(
                    status = HttpStatus.TOO_MANY_REQUESTS.value(),
                    code = "RATE_LIMITED",
                    message = "Too many requests. Please try again later.",
                    timestamp = OffsetDateTime.now().toString()
                )
            )
        )
    }

    private fun maybeCleanup() {
        val now = System.currentTimeMillis()
        val last = lastCleanupMs.get()
        if (now - last >= CLEANUP_INTERVAL_MS && lastCleanupMs.compareAndSet(last, now)) {
            val cutoff = Instant.now()
            buckets.entries.removeIf { !cutoff.isBefore(it.value.expiresAt) }
        }
    }
}