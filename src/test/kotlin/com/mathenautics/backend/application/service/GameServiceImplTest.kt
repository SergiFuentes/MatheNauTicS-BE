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
        // Given
        val request = GameResultRequest(testGameMode, testScore, testCoinsEarned, testDuration)
        val expectedResult = GameSessionResult(testTotalCoins, testSessionId)

        every { userRepository.existsById(testUserId) } returns true
        every {
            gameSessionRepository.saveGameSession(
                testUserId,
                testGameMode,
                testScore,
                testCoinsEarned,
                testDuration
            )
        } returns expectedResult

        // When
        val response = gameSessionService.finishGame(testUserId, request)

        // Then
        assertEquals(testSessionId, response.sessionId)
        assertEquals(testUserId, response.userId)
        assertEquals(testScore, response.score)
        assertEquals(testTotalCoins, response.totalCoins)

        verify(exactly = 1) { userRepository.existsById(testUserId) }
        verify(exactly = 1) {
            gameSessionRepository.saveGameSession(
                testUserId,
                testGameMode,
                testScore,
                testCoinsEarned,
                testDuration
            )
        }
    }

    @Test
    fun `finishGame with zero values should return GameResultResponse`() {
        // Given
        val request = GameResultRequest(testGameMode, 0, 0, 0)
        val expectedResult = GameSessionResult(0, testSessionId)

        every { userRepository.existsById(testUserId) } returns true
        every {
            gameSessionRepository.saveGameSession(
                testUserId,
                testGameMode,
                0,
                0,
                0
            )
        } returns expectedResult

        // When
        val response = gameSessionService.finishGame(testUserId, request)

        // Then
        assertEquals(0, response.score)
        assertEquals(0, response.totalCoins)
    }

    @Test
    fun `finishGame with negative score should throw IllegalArgumentException`() {
        // Given
        val request = GameResultRequest(testGameMode, -1, testCoinsEarned, testDuration)

        // When / Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Score cannot be negative", exception.message)
        verify(exactly = 0) { userRepository.existsById(any()) }
        verify(exactly = 0) { gameSessionRepository.saveGameSession(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `finishGame with negative coins should throw IllegalArgumentException`() {
        // Given
        val request = GameResultRequest(testGameMode, testScore, -5, testDuration)

        // When / Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Coins earned cannot be negative", exception.message)
        verify(exactly = 0) { userRepository.existsById(any()) }
        verify(exactly = 0) { gameSessionRepository.saveGameSession(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `finishGame with negative duration should throw IllegalArgumentException`() {
        // Given
        val request = GameResultRequest(testGameMode, testScore, testCoinsEarned, -10)

        // When / Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("Duration cannot be negative", exception.message)
        verify(exactly = 0) { userRepository.existsById(any()) }
        verify(exactly = 0) { gameSessionRepository.saveGameSession(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `finishGame with non-existent user should throw IllegalArgumentException`() {
        // Given
        val request = GameResultRequest(testGameMode, testScore, testCoinsEarned, testDuration)

        every { userRepository.existsById(testUserId) } returns false

        // When / Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.finishGame(testUserId, request)
        }
        assertEquals("User does not exist", exception.message)
        verify(exactly = 1) { userRepository.existsById(testUserId) }
        verify(exactly = 0) { gameSessionRepository.saveGameSession(any(), any(), any(), any(), any()) }
    }

    // ===================== getCurrentCoins =====================

    @Test
    fun `getCurrentCoins should return PlayerCoinsResponse with total coins`() {
        // Given
        every { gameSessionRepository.getCurrentCoins(testUserId) } returns 150

        // When
        val response = gameSessionService.getCurrentCoins(testUserId)

        // Then
        assertEquals(testUserId, response.userId)
        assertEquals(150, response.totalCoins)
        verify(exactly = 1) { gameSessionRepository.getCurrentCoins(testUserId) }
    }

    @Test
    fun `getCurrentCoins with no coins should return zero`() {
        // Given
        every { gameSessionRepository.getCurrentCoins(testUserId) } returns 0

        // When
        val response = gameSessionService.getCurrentCoins(testUserId)

        // Then
        assertEquals(0, response.totalCoins)
    }

    // ===================== getLeaderboard =====================

    @Test
    fun `getLeaderboard should return list of entries`() {
        // Given
        val limit = 10
        val offset = 0
        val entries = listOf(
            LeaderboardEntry("player1", 100, 50, OffsetDateTime.now(), "adventure"),
            LeaderboardEntry("player2", 90, 45, OffsetDateTime.now(), "adventure")
        )

        every { gameSessionRepository.getLeaderboard(limit, offset, null) } returns entries

        // When
        val result = gameSessionService.getLeaderboard(limit, offset)

        // Then
        assertEquals(2, result.size)
        assertEquals("player1", result[0].username)
        assertEquals(100, result[0].score)
        verify(exactly = 1) { gameSessionRepository.getLeaderboard(limit, offset, null) }
    }

    @Test
    fun `getLeaderboard with gameMode filter should pass filter to repository`() {
        // Given
        val limit = 5
        val offset = 0
        val gameMode = "training"
        val entries = listOf(
            LeaderboardEntry("player3", 200, 100, OffsetDateTime.now(), "training")
        )

        every { gameSessionRepository.getLeaderboard(limit, offset, gameMode) } returns entries

        // When
        val result = gameSessionService.getLeaderboard(limit, offset, gameMode)

        // Then
        assertEquals(1, result.size)
        assertEquals("training", result[0].gameMode)
        verify(exactly = 1) { gameSessionRepository.getLeaderboard(limit, offset, gameMode) }
    }

    @Test
    fun `getLeaderboard with empty result should return empty list`() {
        // Given
        every { gameSessionRepository.getLeaderboard(10, 0, null) } returns emptyList()

        // When
        val result = gameSessionService.getLeaderboard(10, 0)

        // Then
        assertTrue(result.isEmpty())
    }

    // ===================== getPlayerProgress =====================

    @Test
    fun `getPlayerProgress with existing progress should return progress`() {
        // Given
        val gameMode = "adventure"
        val expectedProgress = PlayerProgressResponse(
            userId = testUserId,
            gameMode = gameMode,
            currentLevel = 5,
            score = 500,
            lives = 2,
            coins = 150,
            difficulty = "hard",
            lastPlayedAt = OffsetDateTime.now()
        )

        every { progressRepository.getProgress(testUserId, gameMode) } returns expectedProgress

        // When
        val response = gameSessionService.getPlayerProgress(testUserId, gameMode)

        // Then
        assertEquals(expectedProgress, response)
        verify(exactly = 1) { progressRepository.getProgress(testUserId, gameMode) }
    }

    @Test
    fun `getPlayerProgress with no progress should return default progress`() {
        // Given
        val gameMode = "adventure"

        every { progressRepository.getProgress(testUserId, gameMode) } returns null

        // When
        val response = gameSessionService.getPlayerProgress(testUserId, gameMode)

        // Then
        assertEquals(testUserId, response.userId)
        assertEquals(gameMode, response.gameMode)
        assertEquals(1, response.currentLevel)
        assertEquals(0, response.score)
        assertEquals(3, response.lives)
        assertEquals(0, response.coins)
        assertEquals("normal", response.difficulty)
        assertNotNull(response.lastPlayedAt)

        verify(exactly = 1) { progressRepository.getProgress(testUserId, gameMode) }
    }

    // ===================== updatePlayerProgress =====================

    @Test
    fun `updatePlayerProgress with valid data should return updated progress`() {
        // Given
        val request = PlayerProgressRequest(
            gameMode = "adventure",
            currentLevel = 3,
            score = 300,
            lives = 3,
            coins = 100,
            difficulty = "normal"
        )
        val expectedResponse = PlayerProgressResponse(
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
                testUserId,
                request.gameMode,
                request.currentLevel,
                request.score,
                request.lives,
                request.coins,
                request.difficulty
            )
        } returns expectedResponse

        // When
        val response = gameSessionService.updatePlayerProgress(testUserId, request)

        // Then
        assertEquals(expectedResponse, response)
        verify(exactly = 1) {
            progressRepository.saveOrUpdate(
                testUserId,
                request.gameMode,
                request.currentLevel,
                request.score,
                request.lives,
                request.coins,
                request.difficulty
            )
        }
    }

    @Test
    fun `updatePlayerProgress with level 0 should throw IllegalArgumentException`() {
        // Given
        val request = PlayerProgressRequest(
            gameMode = "adventure",
            currentLevel = 0
        )

        // When / Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.updatePlayerProgress(testUserId, request)
        }
        assertEquals("Level must be at least 1", exception.message)
        verify(exactly = 0) { progressRepository.saveOrUpdate(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `updatePlayerProgress with negative level should throw IllegalArgumentException`() {
        // Given
        val request = PlayerProgressRequest(
            gameMode = "adventure",
            currentLevel = -5
        )

        // When / Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameSessionService.updatePlayerProgress(testUserId, request)
        }
        assertEquals("Level must be at least 1", exception.message)
        verify(exactly = 0) { progressRepository.saveOrUpdate(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `updatePlayerProgress with default values should pass them to repository`() {
        // Given
        val request = PlayerProgressRequest(
            gameMode = "training",
            currentLevel = 1
            // score, lives, coins, difficulty use defaults from constructor
        )
        val expectedResponse = PlayerProgressResponse(
            userId = testUserId,
            gameMode = "training",
            currentLevel = 1,
            score = 0,
            lives = 3,
            coins = 0,
            difficulty = "normal",
            lastPlayedAt = OffsetDateTime.now()
        )

        every {
            progressRepository.saveOrUpdate(
                testUserId,
                "training",
                1,
                0,
                3,
                0,
                "normal"
            )
        } returns expectedResponse

        // When
        val response = gameSessionService.updatePlayerProgress(testUserId, request)

        // Then
        assertEquals(0, response.score)
        assertEquals(3, response.lives)
        assertEquals(0, response.coins)
        assertEquals("normal", response.difficulty)
        verify(exactly = 1) {
            progressRepository.saveOrUpdate(
                testUserId,
                "training",
                1,
                0,
                3,
                0,
                "normal"
            )
        }
    }
}