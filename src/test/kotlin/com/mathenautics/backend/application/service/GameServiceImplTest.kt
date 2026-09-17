package com.mathenautics.backend.application.service

import com.mathenautics.backend.application.service.impl.GameSessionServiceImpl
import com.mathenautics.backend.domain.repository.GameSessionRepository
import com.mathenautics.backend.domain.repository.PlayerProgressRepository
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class GameSessionServiceImplTest {

    private lateinit var gameSessionRepository: GameSessionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var progressRepository: PlayerProgressRepository
    private lateinit var gameSessionService: GameSessionServiceImpl

    private val testUserId = UUID.randomUUID()
    private val testGameMode = "adventure"
    private val testScore = 100
    private val testCoinsEarned = 50
    private val testDuration = 60
    private val testSessionId = 12345L
    private val testTotalCoins = 200
    private val testSessionToken = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        gameSessionRepository = mockk()
        userRepository = mockk()
        progressRepository = mockk()
        gameSessionService = GameSessionServiceImpl(
            gameSessionRepository,
            userRepository,
            progressRepository
        )
    }

    // ===================== finishGame =====================

    @Test
    fun `finishGame with valid data should return GameResultResponse`() {
        val request = GameResultRequest(testGameMode, testScore, testCoinsEarned, testDuration, testSessionToken)
        val expectedResult = GameSessionResult(testTotalCoins, testSessionId)

        every { userRepository.existsById(testUserId) } returns true
        every {
            gameSessionRepository.saveGameSession(
                testUserId,
                testGameMode,
                testScore,
                testCoinsEarned,
                testDuration,
                testSessionToken
            )
        } returns expectedResult

        val response = gameSessionService.finishGame(testUserId, request)

        assertEquals(testSessionId, response.sessionId)
        assertEquals(testUserId, response.userId)
        assertEquals(testScore, response.score)
        assertEquals(testTotalCoins, response.totalCoins)

        verify(exactly = 1) {
            gameSessionRepository.saveGameSession(
                testUserId,
                testGameMode,
                testScore,
                testCoinsEarned,
                testDuration,
                testSessionToken
            )
        }
    }

    @Test
    fun `finishGame with zero values should return GameResultResponse`() {
        val request = GameResultRequest(testGameMode, 0, 0, 0, testSessionToken)
        val expectedResult = GameSessionResult(0, testSessionId)

        every { userRepository.existsById(testUserId) } returns true
        every {
            gameSessionRepository.saveGameSession(
                testUserId,
                testGameMode,
                0,
                0,
                0,
                testSessionToken
            )
        } returns expectedResult

        val response = gameSessionService.finishGame(testUserId, request)

        assertEquals(0, response.score)
        assertEquals(0, response.totalCoins)
    }

    @Test
    fun `finishGame with negative score should throw IllegalArgumentException`() {
        val request = GameResultRequest(testGameMode, -1, testCoinsEarned, testDuration, testSessionToken)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Score cannot be negative", ex.message)
        verify(exactly = 0) { userRepository.existsById(any()) }
    }

    @Test
    fun `finishGame with excessive score should throw IllegalArgumentException`() {
        val request = GameResultRequest(testGameMode, 999_999_999, testCoinsEarned, testDuration, testSessionToken)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Score exceeds the maximum allowed value", ex.message)
    }

    @Test
    fun `finishGame with negative coins should throw IllegalArgumentException`() {
        val request = GameResultRequest(testGameMode, testScore, -5, testDuration, testSessionToken)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Coins earned cannot be negative", ex.message)
    }

    @Test
    fun `finishGame with excessive coins should throw IllegalArgumentException`() {
        val request = GameResultRequest(testGameMode, testScore, 99_999, testDuration, testSessionToken)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Coins earned exceed the maximum allowed value", ex.message)
    }

    @Test
    fun `finishGame with negative duration should throw IllegalArgumentException`() {
        val request = GameResultRequest(testGameMode, testScore, testCoinsEarned, -10, testSessionToken)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Duration cannot be negative", ex.message)
    }

    @Test
    fun `finishGame with excessive duration should throw IllegalArgumentException`() {
        val request = GameResultRequest(testGameMode, testScore, testCoinsEarned, 86_400, testSessionToken)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Duration exceeds the maximum allowed value", ex.message)
    }

    @Test
    fun `finishGame with invalid game mode should throw IllegalArgumentException`() {
        val request = GameResultRequest("hacker", testScore, testCoinsEarned, testDuration, testSessionToken)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Invalid game mode", ex.message)
    }

    @Test
    fun `finishGame with non-existent user should throw IllegalArgumentException`() {
        val request = GameResultRequest(testGameMode, testScore, testCoinsEarned, testDuration, testSessionToken)
        every { userRepository.existsById(testUserId) } returns false

        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("User does not exist", ex.message)
    }

    // ===================== getCurrentCoins =====================

    @Test
    fun `getCurrentCoins should return PlayerCoinsResponse with total coins`() {
        every { gameSessionRepository.getCurrentCoins(testUserId) } returns 150
        val response = gameSessionService.getCurrentCoins(testUserId)
        assertEquals(testUserId, response.userId)
        assertEquals(150, response.totalCoins)
    }

    // ===================== getLeaderboard =====================

    @Test
    fun `getLeaderboard should return list of entries`() {
        val entries = listOf(
            LeaderboardEntry("player1", 100, 50, OffsetDateTime.now(), "adventure")
        )
        every { gameSessionRepository.getLeaderboard(10, 0, null) } returns entries

        val result = gameSessionService.getLeaderboard(10, 0)
        assertEquals(1, result.size)
        assertEquals("player1", result[0].username)
    }

    // ===================== getPlayerProgress =====================

    @Test
    fun `getPlayerProgress with no progress should return default progress`() {
        every { progressRepository.getProgress(testUserId, "adventure") } returns null
        val response = gameSessionService.getPlayerProgress(testUserId, "adventure")

        assertEquals(1, response.currentLevel)
        assertEquals(3, response.lives)
        assertEquals("normal", response.difficulty)
    }

    // ===================== updatePlayerProgress =====================

    @Test
    fun `updatePlayerProgress with valid data should return updated progress`() {
        val request = PlayerProgressRequest("adventure", 3, 300, 3, 100, "normal")
        val expected = PlayerProgressResponse(
            userId = testUserId,
            gameMode = "adventure",
            currentLevel = 3,
            score = 300,
            lives = 3,
            coins = 100,
            difficulty = "normal",
            lastPlayedAt = OffsetDateTime.now()
        )
        every {
            progressRepository.saveOrUpdate(
                testUserId, "adventure", 3, 300, 3, 100, "normal"
            )
        } returns expected

        val response = gameSessionService.updatePlayerProgress(testUserId, request)
        assertEquals(expected, response)
    }

    @Test
    fun `updatePlayerProgress with level 0 should throw IllegalArgumentException`() {
        val request = PlayerProgressRequest("adventure", 0)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.updatePlayerProgress(testUserId, request)
        }
        assertEquals("Level must be at least 1", ex.message)
    }

    @Test
    fun `updatePlayerProgress with excessive level should throw IllegalArgumentException`() {
        val request = PlayerProgressRequest("adventure", 999_999)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.updatePlayerProgress(testUserId, request)
        }
        assertEquals("Level exceeds the maximum allowed value", ex.message)
    }

    @Test
    fun `updatePlayerProgress with invalid game mode should throw IllegalArgumentException`() {
        val request = PlayerProgressRequest("hacker", 1)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.updatePlayerProgress(testUserId, request)
        }
        assertEquals("Invalid game mode", ex.message)
    }

    @Test
    fun `updatePlayerProgress with invalid difficulty should throw IllegalArgumentException`() {
        val request = PlayerProgressRequest("adventure", 1, 0, 3, 0, "hacker")
        val ex = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.updatePlayerProgress(testUserId, request)
        }
        assertEquals("Invalid difficulty", ex.message)
    }
}