package com.mathenautics.backend.application.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mathenautics.backend.application.service.GameSessionService
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.GameResultRequest
import com.mathenautics.backend.dto.GameResultResponse
import com.mathenautics.backend.dto.LeaderboardEntry
import com.mathenautics.backend.dto.PlayerCoinsResponse
import com.mathenautics.backend.dto.PlayerProgressRequest
import com.mathenautics.backend.dto.PlayerProgressResponse
import com.mathenautics.backend.security.AuthenticatedUser
import com.mathenautics.backend.security.JwtService
import com.mathenautics.backend.security.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.util.UUID

@WebMvcTest(GameController::class)
@Import(SecurityConfig::class)
class GameControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var gameService: GameSessionService

    @MockkBean
    private lateinit var jwtService: JwtService

    @MockkBean
    private lateinit var userRepository: UserRepository

    private val testUserId = UUID.randomUUID()
    private val testUsername = "testuser"
    private val testSessionToken = UUID.randomUUID()

    private val authenticatedUser = AuthenticatedUser(
        userId = testUserId,
        username = testUsername,
        isGuest = false
    )

    private val authentication = mockk<Authentication> {
        every { principal } returns authenticatedUser
        every { isAuthenticated } returns true
        every { name } returns testUsername
    }

    @Test
    fun `finishGame should return GameResultResponse`() {
        val request = GameResultRequest("adventure", 100, 50, 60, testSessionToken)

        val response = GameResultResponse(
            sessionId = 12345L,
            userId = testUserId,
            score = 100,
            totalCoins = 50
        )

        every { gameService.finishGame(testUserId, any()) } returns response

        mockMvc.perform(
            post("/api/v1/games/finish")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionId").value(12345L))
            .andExpect(jsonPath("$.userId").value(testUserId.toString()))
            .andExpect(jsonPath("$.score").value(100))
            .andExpect(jsonPath("$.totalCoins").value(50))

        verify(exactly = 1) {
            gameService.finishGame(
                testUserId,
                match {
                    it.gameMode == "adventure" &&
                            it.score == 100 &&
                            it.coinsEarned == 50 &&
                            it.durationSeconds == 60 &&
                            it.sessionToken == testSessionToken
                }
            )
        }
    }

    @Test
    fun `getPlayerCoins should return PlayerCoinsResponse`() {
        val response = PlayerCoinsResponse(testUserId, 150)
        every { gameService.getCurrentCoins(testUserId) } returns response

        mockMvc.perform(
            get("/api/v1/games/player/coins").with(authentication(authentication))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(testUserId.toString()))
            .andExpect(jsonPath("$.totalCoins").value(150))

        verify(exactly = 1) { gameService.getCurrentCoins(testUserId) }
    }

    @Test
    fun `getLeaderboard should return list of entries`() {
        val entries = listOf(
            LeaderboardEntry("player1", 100, 50, OffsetDateTime.now(), "adventure"),
            LeaderboardEntry("player2", 90, 45, OffsetDateTime.now(), "adventure")
        )

        every { gameService.getLeaderboard(10, 0, null) } returns entries

        mockMvc.perform(
            get("/api/v1/games/leaderboard")
                .param("limit", "10")
                .param("offset", "0")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(2))
            .andExpect(jsonPath("$[0].username").value("player1"))
            .andExpect(jsonPath("$[0].score").value(100))
            .andExpect(jsonPath("$[1].username").value("player2"))
    }

    @Test
    fun `getProgress should return PlayerProgressResponse`() {
        val progress = PlayerProgressResponse(
            userId = testUserId,
            gameMode = "adventure",
            currentLevel = 5,
            score = 300,
            lives = 2,
            coins = 100,
            difficulty = "pro",
            lastPlayedAt = OffsetDateTime.now()
        )

        every { gameService.getPlayerProgress(testUserId, "adventure") } returns progress

        mockMvc.perform(
            get("/api/v1/games/progress")
                .with(authentication(authentication))
                .param("gameMode", "adventure")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentLevel").value(5))
            .andExpect(jsonPath("$.difficulty").value("pro"))
    }

    @Test
    fun `updateProgress should return updated PlayerProgressResponse`() {
        val request = PlayerProgressRequest("adventure", 6, 400, 3, 150, "pro")

        val response = PlayerProgressResponse(
            userId = testUserId,
            gameMode = "adventure",
            currentLevel = 6,
            score = 400,
            lives = 3,
            coins = 150,
            difficulty = "pro",
            lastPlayedAt = OffsetDateTime.now()
        )

        every { gameService.updatePlayerProgress(testUserId, any()) } returns response

        mockMvc.perform(
            post("/api/v1/games/progress")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentLevel").value(6))
            .andExpect(jsonPath("$.difficulty").value("pro"))
    }

    @Test
    fun `finishGame should return 401 when unauthenticated`() {
        mockMvc.perform(
            post("/api/v1/games/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        GameResultRequest("adventure", 100, 50, 60, UUID.randomUUID())
                    )
                )
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getPlayerCoins should return 401 when unauthenticated`() {
        mockMvc.perform(get("/api/v1/games/player/coins"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getLeaderboard should be accessible without authentication`() {
        every { gameService.getLeaderboard(10, 0, null) } returns emptyList()

        mockMvc.perform(get("/api/v1/games/leaderboard"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(0))
    }

    @Test
    fun `getProgress should return 401 when unauthenticated`() {
        mockMvc.perform(get("/api/v1/games/progress").param("gameMode", "adventure"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `updateProgress should return 401 when unauthenticated`() {
        mockMvc.perform(
            post("/api/v1/games/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        PlayerProgressRequest("adventure", 6, 400, 3, 150, "pro")
                    )
                )
        )
            .andExpect(status().isUnauthorized)
    }
}