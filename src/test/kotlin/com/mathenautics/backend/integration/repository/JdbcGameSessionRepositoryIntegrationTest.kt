package com.mathenautics.backend.integration.repository

import com.mathenautics.backend.infrastructure.repository.JdbcGameSessionRepository
import com.mathenautics.backend.integration.IntegrationTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcGameSessionRepositoryIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var gameSessionRepository: JdbcGameSessionRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val testUserId = UUID.randomUUID()
    private val testGameMode = "adventure"
    private val testScore = 100
    private val testCoinsEarned = 50
    private val testDuration = 60

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM game_sessions")
        jdbcTemplate.execute("DELETE FROM users")
        // Insert a test user
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, email, password_hash, is_guest, created_at)
            VALUES (?, 'testuser', 'test@example.com', 'hash', false, NOW())
            """,
            testUserId
        )
    }

    @Test
    fun `saveGameSession should insert session and return total coins`() {
        // Given

        // When
        val result = gameSessionRepository.saveGameSession(
            testUserId,
            testGameMode,
            testScore,
            testCoinsEarned,
            testDuration
        )

        // Then
        assertNotNull(result.sessionId)
        assertTrue(result.sessionId > 0)
        assertEquals(testCoinsEarned, result.totalCoins)

        // Verify in database
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM game_sessions WHERE user_id = ?",
            Int::class.java,
            testUserId
        ) ?: 0
        assertEquals(1, count)

        // Check total coins in user's sessions
        val totalCoins = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_coins), 0) FROM game_sessions WHERE user_id = ?",
            Int::class.java,
            testUserId
        ) ?: 0
        assertEquals(testCoinsEarned, totalCoins)
    }

    @Test
    fun `saveGameSession multiple times should accumulate total coins`() {
        // When
        gameSessionRepository.saveGameSession(testUserId, testGameMode, 100, 50, 60)
        gameSessionRepository.saveGameSession(testUserId, testGameMode, 200, 30, 45)

        // Then
        val totalCoins = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_coins), 0) FROM game_sessions WHERE user_id = ?",
            Int::class.java,
            testUserId
        ) ?: 0
        assertEquals(80, totalCoins)
    }

    @Test
    fun `getCurrentCoins should return sum of coins earned`() {
        // Given
        gameSessionRepository.saveGameSession(testUserId, testGameMode, 100, 50, 60)
        gameSessionRepository.saveGameSession(testUserId, testGameMode, 200, 30, 45)

        // When
        val totalCoins = gameSessionRepository.getCurrentCoins(testUserId)

        // Then
        assertEquals(80, totalCoins)
    }

    @Test
    fun `getCurrentCoins should return 0 if no sessions`() {
        // When
        val totalCoins = gameSessionRepository.getCurrentCoins(testUserId)

        // Then
        assertEquals(0, totalCoins)
    }

    @Test
    fun `getLeaderboard should return sorted entries`() {
        // Given
        val user2Id = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, email, password_hash, is_guest, created_at)
            VALUES (?, 'user2', 'user2@example.com', 'hash', false, NOW())
            """,
            user2Id
        )

        // Insert sessions for user1 (score 100, coins 50) and user2 (score 200, coins 100)
        gameSessionRepository.saveGameSession(testUserId, "adventure", 100, 50, 60)
        Thread.sleep(10) // ensure different timestamps
        gameSessionRepository.saveGameSession(user2Id, "adventure", 200, 100, 45)

        // When
        val leaderboard = gameSessionRepository.getLeaderboard(limit = 10, offset = 0)

        // Then
        assertEquals(2, leaderboard.size)
        // Highest score first (user2)
        assertEquals("user2", leaderboard[0].username)
        assertEquals(200, leaderboard[0].score)
        assertEquals(100, leaderboard[0].totalCoins)
        assertEquals("adventure", leaderboard[0].gameMode)

        assertEquals("testuser", leaderboard[1].username)
        assertEquals(100, leaderboard[1].score)
        assertEquals(50, leaderboard[1].totalCoins)
    }

    @Test
    fun `getLeaderboard should filter by gameMode`() {
        // Given
        val user2Id = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, email, password_hash, is_guest, created_at)
            VALUES (?, 'user2', 'user2@example.com', 'hash', false, NOW())
            """,
            user2Id
        )

        gameSessionRepository.saveGameSession(testUserId, "adventure", 100, 50, 60)
        gameSessionRepository.saveGameSession(user2Id, "training", 200, 100, 45)

        // When
        val leaderboard = gameSessionRepository.getLeaderboard(limit = 10, offset = 0, gameMode = "adventure")

        // Then
        assertEquals(1, leaderboard.size)
        assertEquals("testuser", leaderboard[0].username)
        assertEquals("adventure", leaderboard[0].gameMode)
    }

    @Test
    fun `getLeaderboard should respect limit and offset`() {
        // Given
        val user2Id = UUID.randomUUID()
        val user3Id = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, email, password_hash, is_guest, created_at)
            VALUES (?, 'user2', 'user2@example.com', 'hash', false, NOW())
            """,
            user2Id
        )
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, email, password_hash, is_guest, created_at)
            VALUES (?, 'user3', 'user3@example.com', 'hash', false, NOW())
            """,
            user3Id
        )

        gameSessionRepository.saveGameSession(testUserId, "adventure", 100, 50, 60)
        gameSessionRepository.saveGameSession(user2Id, "adventure", 200, 100, 45)
        gameSessionRepository.saveGameSession(user3Id, "adventure", 150, 75, 30)

        // When
        val leaderboard = gameSessionRepository.getLeaderboard(limit = 2, offset = 1)

        // Then
        assertEquals(2, leaderboard.size)
        // Ordered by score DESC: 200, 150, 100. Offset 1 => skip 200, get 150 and 100
        assertEquals("user3", leaderboard[0].username)
        assertEquals(150, leaderboard[0].score)
        assertEquals("testuser", leaderboard[1].username)
        assertEquals(100, leaderboard[1].score)
    }
}