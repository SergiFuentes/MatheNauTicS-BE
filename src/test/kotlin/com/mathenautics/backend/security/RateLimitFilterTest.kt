package com.mathenautics.backend.security

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RateLimitFilterTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private fun buildFilter(
        loginMax: Int = 3,
        loginWindow: Int = 60,
        registrationMax: Int = 2,
        registrationWindow: Int = 60,
        maxBuckets: Int = RateLimitFilter.DEFAULT_MAX_BUCKETS
    ): RateLimitFilter = RateLimitFilter(
        RateLimitProperties(
            login = RateLimitProperties.Rule(loginMax, loginWindow),
            registration = RateLimitProperties.Rule(registrationMax, registrationWindow)
        ),
        objectMapper,
        maxBuckets
    )

    private fun loginRequest(ip: String): MockHttpServletRequest =
        MockHttpServletRequest("POST", RateLimitFilter.LOGIN_PATH).apply {
            addHeader("X-Forwarded-For", ip)
            contentType = "application/json"
            setContent("""{"identifier":"x","password":"y"}""".toByteArray())
        }

    private fun registrationRequest(ip: String): MockHttpServletRequest =
        MockHttpServletRequest("POST", RateLimitFilter.REGISTRATION_PATH).apply {
            addHeader("X-Forwarded-For", ip)
            contentType = "application/json"
            setContent("""{"username":"x","email":"x@example.io","password":"yyyyyy"}""".toByteArray())
        }

    private fun fire(
        filter: RateLimitFilter,
        request: MockHttpServletRequest
    ): MockHttpServletResponse {
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())
        return response
    }

    @Test
    fun `login below threshold is allowed`() {
        val filter = buildFilter()
        repeat(3) {
            assertEquals(200, fire(filter, loginRequest("1.2.3.4")).status)
        }
    }

    @Test
    fun `login above threshold is rejected with 429 and Retry-After`() {
        val filter = buildFilter()
        repeat(3) { fire(filter, loginRequest("1.2.3.4")) }

        val res = fire(filter, loginRequest("1.2.3.4"))
        assertEquals(429, res.status)
        assertNotNull(res.getHeader("Retry-After"))
        assertTrue(res.contentAsString.contains("\"code\":\"RATE_LIMITED\""))
    }

    @Test
    fun `registration above threshold is rejected with 429`() {
        val filter = buildFilter()
        repeat(2) {
            assertEquals(200, fire(filter, registrationRequest("5.6.7.8")).status)
        }
        assertEquals(429, fire(filter, registrationRequest("5.6.7.8")).status)
    }

    @Test
    fun `different IPs have independent buckets`() {
        val filter = buildFilter()
        repeat(4) { fire(filter, loginRequest("1.2.3.4")) }
        assertEquals(429, fire(filter, loginRequest("1.2.3.4")).status)
        assertEquals(200, fire(filter, loginRequest("5.6.7.8")).status)
    }

    @Test
    fun `login and registration use separate buckets per IP`() {
        val filter = buildFilter()
        repeat(4) { fire(filter, loginRequest("1.2.3.4")) }
        assertEquals(429, fire(filter, loginRequest("1.2.3.4")).status)
        assertEquals(200, fire(filter, registrationRequest("1.2.3.4")).status)
    }

    @Test
    fun `window expiry resets counter`() {
        val filter = buildFilter(loginMax = 2, loginWindow = 1)
        repeat(3) { fire(filter, loginRequest("1.2.3.4")) }
        assertEquals(429, fire(filter, loginRequest("1.2.3.4")).status)

        Thread.sleep(1100)

        assertEquals(200, fire(filter, loginRequest("1.2.3.4")).status)
    }

    @Test
    fun `full bucket store rejects new keys but preserves active buckets`() {
        val filter = buildFilter(loginMax = 3, maxBuckets = 2)

        assertEquals(200, fire(filter, loginRequest("1.2.3.4")).status)
        assertEquals(200, fire(filter, loginRequest("5.6.7.8")).status)

        val rejected = fire(filter, loginRequest("9.10.11.12"))
        assertEquals(429, rejected.status)
        assertNotNull(rejected.getHeader("Retry-After"))

        // Existing keys keep their original bucket and are not evicted to make
        // room for a new client.
        assertEquals(200, fire(filter, loginRequest("1.2.3.4")).status)
        assertEquals(200, fire(filter, loginRequest("5.6.7.8")).status)
    }

    @Test
    fun `expired buckets are reclaimed when capacity is reached`() {
        val filter = buildFilter(loginMax = 3, loginWindow = 1, maxBuckets = 1)

        assertEquals(200, fire(filter, loginRequest("1.2.3.4")).status)
        assertEquals(429, fire(filter, loginRequest("5.6.7.8")).status)

        Thread.sleep(1100)

        assertEquals(200, fire(filter, loginRequest("5.6.7.8")).status)
    }

    @Test
    fun `concurrent new keys never exceed bucket capacity`() {
        val capacity = 8
        val requestCount = 40
        val filter = buildFilter(loginMax = 100, maxBuckets = capacity)
        val ready = CountDownLatch(requestCount)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(requestCount)

        try {
            val tasks = (0 until requestCount).map { index ->
                Callable {
                    ready.countDown()
                    check(start.await(5, TimeUnit.SECONDS))
                    fire(filter, loginRequest("192.0.2.$index")).status
                }
            }
            val futures = tasks.map(executor::submit)
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()

            val statuses = futures.map { it.get(5, TimeUnit.SECONDS) }
            assertEquals(capacity, statuses.count { it == 200 })
            assertEquals(requestCount - capacity, statuses.count { it == 429 })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `non-authentication endpoints are never rate limited`() {
        val filter = buildFilter()
        val paths = listOf(
            "/api/v1/games/finish",
            "/api/v1/games/progress",
            "/api/v1/users/me/convert",
            "/api/v1/games/player/coins"
        )
        for (path in paths) {
            repeat(50) {
                val req = MockHttpServletRequest("POST", path).apply {
                    addHeader("X-Forwarded-For", "1.2.3.4")
                }
                assertEquals(200, fire(filter, req).status)
            }
        }
    }

    @Test
    fun `non-POST requests to login path are never rate limited`() {
        val filter = buildFilter()
        repeat(20) {
            val req = MockHttpServletRequest("GET", RateLimitFilter.LOGIN_PATH).apply {
                addHeader("X-Forwarded-For", "1.2.3.4")
            }
            assertEquals(200, fire(filter, req).status)
        }
    }

    @Test
    fun `X-Forwarded-For first IP is used as key`() {
        val filter = buildFilter(loginMax = 1)

        val r1 = MockHttpServletRequest("POST", RateLimitFilter.LOGIN_PATH).apply {
            addHeader("X-Forwarded-For", "1.1.1.1, 10.0.0.1, 10.0.0.2")
        }
        assertEquals(200, fire(filter, r1).status)

        val r2 = MockHttpServletRequest("POST", RateLimitFilter.LOGIN_PATH).apply {
            addHeader("X-Forwarded-For", "1.1.1.1, 10.0.0.9")
        }
        assertEquals(429, fire(filter, r2).status)

        val r3 = MockHttpServletRequest("POST", RateLimitFilter.LOGIN_PATH).apply {
            addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.1")
        }
        assertEquals(200, fire(filter, r3).status)
    }

    @Test
    fun `missing X-Forwarded-For falls back to remoteAddr`() {
        val filter = buildFilter(loginMax = 1)

        val r1 = MockHttpServletRequest("POST", RateLimitFilter.LOGIN_PATH).apply {
            remoteAddr = "4.4.4.4"
        }
        assertEquals(200, fire(filter, r1).status)

        val r2 = MockHttpServletRequest("POST", RateLimitFilter.LOGIN_PATH).apply {
            remoteAddr = "4.4.4.4"
        }
        assertEquals(429, fire(filter, r2).status)
    }

    @Test
    fun `Retry-After header is a positive integer within the window`() {
        val filter = buildFilter(loginMax = 1, loginWindow = 60)
        fire(filter, loginRequest("1.2.3.4"))

        val res = fire(filter, loginRequest("1.2.3.4"))
        val header = res.getHeader("Retry-After")
            ?: error("Retry-After header missing")
        val seconds = header.toLongOrNull()
            ?: error("Retry-After must be an integer")

        assertTrue(seconds in 1L..60L)
    }
}