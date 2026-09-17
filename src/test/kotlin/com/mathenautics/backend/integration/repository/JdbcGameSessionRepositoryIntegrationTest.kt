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
        val sessionToken = UUID.randomUUID()

        val result = gameSessionRepository.saveGameSession(
            testUserId,
            testGameMode,
            testScore,
            testCoinsEarned,
            testDuration,
            sessionToken
        )

        assertNotNull(result.sessionId)
        assertTrue(result.sessionId > 0)
        assertEquals(testCoinsEarned, result.totalCoins)

        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM game_sessions WHERE user_id = ?",
            Int::class.java,
            testUserId
        ) ?: 0
        assertEquals(1, count)

        val totalCoins = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_coins), 0) FROM game_sessions WHERE user_id = ?",
            Int::class.java,
            testUserId
        ) ?: 0
        assertEquals(testCoinsEarned, totalCoins)
    }

    @Test
    fun `saveGameSession multiple times should accumulate total coins`() {
        gameSessionRepository.saveGameSession(
            testUserId, testGameMode, 100, 50, 60, UUID.randomUUID()
        )
        gameSessionRepository.saveGameSession(
            testUserId, testGameMode, 200, 30, 45, UUID.randomUUID()
        )

        val totalCoins = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_coins), 0) FROM game_sessions WHERE user_id = ?",
            Int::class.java,
            testUserId
        ) ?: 0
        assertEquals(80, totalCoins)
    }

    @Test
    fun `saveGameSession with the same sessionToken is idempotent`() {
        val sessionToken = UUID.randomUUID()

        val first = gameSessionRepository.saveGameSession(
            testUserId, testGameMode, 100, 50, 60, sessionToken
        )
        val second = gameSessionRepository.saveGameSession(
            testUserId, testGameMode, 100, 50, 60, sessionToken
        )

        assertEquals(first.sessionId, second.sessionId)
        assertEquals(first.totalCoins, second.totalCoins)

        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM game_sessions WHERE user_id = ?",
            Int::class.java,
            testUserId
        ) ?: 0
        assertEquals(1, count)

        val totalCoins = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_coins), 0) FROM game_sessions WHERE user_id = ?",
            Int::class.java,
            testUserId
        ) ?: 0
        assertEquals(50, totalCoins)
    }

    @Test
    fun `getCurrentCoins should return sum of coins earned`() {
        gameSessionRepository.saveGameSession(
            testUserId, testGameMode, 100, 50, 60, UUID.randomUUID()
        )
        gameSessionRepository.saveGameSession(
            testUserId, testGameMode, 200, 30, 45, UUID.randomUUID()
        )

        val totalCoins = gameSessionRepository.getCurrentCoins(testUserId)

        assertEquals(80, totalCoins)
    }

    @Test
    fun `getCurrentCoins should return 0 if no sessions`() {
        val totalCoins = gameSessionRepository.getCurrentCoins(testUserId)
        assertEquals(0, totalCoins)
    }

    @Test
    fun `getLeaderboard should return sorted entries`() {
        val user2Id = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, email, password_hash, is_guest, created_at)
            VALUES (?, 'user2', 'user2@example.com', 'hash', false, NOW())
            """,
            user2Id
        )

        gameSessionRepository.saveGameSession(
            testUserId, "adventure", 100, 50, 60, UUID.randomUUID()
        )
        Thread.sleep(10)
        gameSessionRepository.saveGameSession(
            user2Id, "adventure", 200, 100, 45, UUID.randomUUID()
        )

        val leaderboard = gameSessionRepository.getLeaderboard(limit = 10, offset = 0)

        assertEquals(2, leaderboard.size)
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
        val user2Id = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, email, password_hash, is_guest, created_at)
            VALUES (?, 'user2', 'user2@example.com', 'hash', false, NOW())
            """,
            user2Id
        )

        gameSessionRepository.saveGameSession(
            testUserId, "adventure", 100, 50, 60, UUID.randomUUID()
        )
        gameSessionRepository.saveGameSession(
            user2Id, "training", 200, 100, 45, UUID.randomUUID()
        )

        val leaderboard = gameSessionRepository.getLeaderboard(
            limit = 10, offset = 0, gameMode = "adventure"
        )

        assertEquals(1, leaderboard.size)
        assertEquals("testuser", leaderboard[0].username)
        assertEquals("adventure", leaderboard[0].gameMode)
    }

    @Test
    fun `getLeaderboard should respect limit and offset`() {
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

        gameSessionRepository.saveGameSession(
            testUserId, "adventure", 100, 50, 60, UUID.randomUUID()
        )
        gameSessionRepository.saveGameSession(
            user2Id, "adventure", 200, 100, 45, UUID.randomUUID()
        )
        gameSessionRepository.saveGameSession(
            user3Id, "adventure", 150, 75, 30, UUID.randomUUID()
        )

        val leaderboard = gameSessionRepository.getLeaderboard(limit = 2, offset = 1)

        assertEquals(2, leaderboard.size)
        assertEquals("user3", leaderboard[0].username)
        assertEquals(150, leaderboard[0].score)
        assertEquals("testuser", leaderboard[1].username)
        assertEquals(100, leaderboard[1].score)
    }
}