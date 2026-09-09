package com.mathenautics.backend.security

import com.mathenautics.backend.integration.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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
}