package com.mathenautics.backend.security

import com.mathenautics.backend.integration.IntegrationTestBase
import java.util.concurrent.ThreadLocalRandom
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `protected endpoint should return 401 without token`() {
        mockMvc.perform(
            get("/api/v1/users/me")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected endpoint should return 401 with invalid token`() {
        mockMvc.perform(
            get("/api/v1/users/me")
                .header("Authorization", "Bearer invalid.token")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected game endpoint should return 401 without token`() {
        mockMvc.perform(
            get("/api/v1/games/player/coins")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected game endpoint should return 401 with invalid token`() {
        mockMvc.perform(
            get("/api/v1/games/player/coins")
                .header("Authorization", "Bearer invalid.token")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected progress endpoint should return 401 without token`() {
        mockMvc.perform(
            get("/api/v1/games/progress")
                .param("gameMode", "adventure")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected progress endpoint should return 401 with invalid token`() {
        mockMvc.perform(
            get("/api/v1/games/progress")
                .param("gameMode", "adventure")
                .header("Authorization", "Bearer invalid.token")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `public login endpoint should not be rejected by security`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(
                    """
                    {
                        "identifier": "test",
                        "password": "test"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `public leaderboard endpoint should be accessible without token`() {
        mockMvc.perform(
            get("/api/v1/games/leaderboard")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `login endpoint is rate limited after configured threshold`() {
        val ip = "198.51.100.${ThreadLocalRandom.current().nextInt(1, 256)}"
        val body = """{"identifier":"nobody@example.io","password":"wrongpass"}"""

        repeat(10) {
            mockMvc.perform(
                post("/api/v1/auth/login")
                    .header("X-Forwarded-For", ip)
                    .contentType("application/json")
                    .content(body)
            ).andExpect(status().isUnauthorized)
        }

        mockMvc.perform(
            post("/api/v1/auth/login")
                .header("X-Forwarded-For", ip)
                .contentType("application/json")
                .content(body)
        )
            .andExpect(status().isTooManyRequests)
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
            .andExpect(jsonPath("$.status").value(429))
    }

    @Test
    fun `registration endpoint is rate limited after configured threshold`() {
        val ip = "198.51.100.${ThreadLocalRandom.current().nextInt(1, 256)}"
        // Invalid payload still consumes a rate-limit slot and returns 400.
        val body = """{"username":"","email":"","password":"","isGuest":true}"""

        repeat(20) {
            mockMvc.perform(
                post("/api/v1/users")
                    .header("X-Forwarded-For", ip)
                    .contentType("application/json")
                    .content(body)
            ).andExpect(status().isBadRequest)
        }

        mockMvc.perform(
            post("/api/v1/users")
                .header("X-Forwarded-For", ip)
                .contentType("application/json")
                .content(body)
        )
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
    }

    @Test
    fun `game and progress endpoints are not affected by the auth rate limiter`() {
        // No JWT, so responses will be 401 — the point is that none of them
        // returns 429 regardless of how many are sent from the same IP.
        val ip = "198.51.100.${ThreadLocalRandom.current().nextInt(1, 256)}"
        repeat(30) {
            mockMvc.perform(
                post("/api/v1/games/progress")
                    .header("X-Forwarded-For", ip)
                    .contentType("application/json")
                    .content("{}")
            ).andExpect(status().isUnauthorized)

            mockMvc.perform(
                get("/api/v1/games/leaderboard")
                    .header("X-Forwarded-For", ip)
            ).andExpect(status().isOk)
        }
    }
}