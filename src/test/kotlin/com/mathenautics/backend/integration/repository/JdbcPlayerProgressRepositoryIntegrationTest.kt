package com.mathenautics.backend.integration.repository

import com.mathenautics.backend.infrastructure.repository.JdbcPlayerProgressRepository
import com.mathenautics.backend.integration.IntegrationTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcPlayerProgressRepositoryIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var progressRepository: JdbcPlayerProgressRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val testUserId = UUID.randomUUID()
    private val testGameMode = "adventure"

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM player_progress")
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
    fun `saveOrUpdate should insert new progress`() {
        // When
        val saved = progressRepository.saveOrUpdate(
            userId = testUserId,
            gameMode = testGameMode,
            level = 5,
            score = 300,
            lives = 2,
            coins = 100,
            difficulty = "hard"
        )

        // Then
        assertEquals(testUserId, saved.userId)
        assertEquals(testGameMode, saved.gameMode)
        assertEquals(5, saved.currentLevel)
        assertEquals(300, saved.score)
        assertEquals(2, saved.lives)
        assertEquals(100, saved.coins)
        assertEquals("hard", saved.difficulty)
        assertNotNull(saved.lastPlayedAt)

        // Verify in database
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM player_progress WHERE user_id = ? AND game_mode = ?",
            Int::class.java,
            testUserId, testGameMode
        ) ?: 0
        assertEquals(1, count)
    }

    @Test
    fun `saveOrUpdate should update existing progress`() {
        // Given - insert initial progress
        val initial = progressRepository.saveOrUpdate(
            userId = testUserId,
            gameMode = testGameMode,
            level = 1,
            score = 0,
            lives = 3,
            coins = 0,
            difficulty = "normal"
        )

        // When - update
        val updated = progressRepository.saveOrUpdate(
            userId = testUserId,
            gameMode = testGameMode,
            level = 10,
            score = 500,
            lives = 1,
            coins = 200,
            difficulty = "expert"
        )

        // Then
        assertEquals(testUserId, updated.userId)
        assertEquals(10, updated.currentLevel)
        assertEquals(500, updated.score)
        assertEquals(1, updated.lives)
        assertEquals(200, updated.coins)
        assertEquals("expert", updated.difficulty)
        assertTrue(updated.lastPlayedAt.isAfter(initial.lastPlayedAt))

        // Verify in database
        val dbLevel = jdbcTemplate.queryForObject(
            "SELECT current_level FROM player_progress WHERE user_id = ? AND game_mode = ?",
            Int::class.java,
            testUserId, testGameMode
        ) ?: 0
        assertEquals(10, dbLevel)
    }

    @Test
    fun `getProgress should return progress for existing user and game mode`() {
        // Given
        progressRepository.saveOrUpdate(
            userId = testUserId,
            gameMode = testGameMode,
            level = 3,
            score = 150,
            lives = 3,
            coins = 50,
            difficulty = "normal"
        )

        // When
        val progress = progressRepository.getProgress(testUserId, testGameMode)

        // Then
        assertNotNull(progress)
        assertEquals(testUserId, progress?.userId)
        assertEquals(testGameMode, progress?.gameMode)
        assertEquals(3, progress?.currentLevel)
        assertEquals(150, progress?.score)
        assertEquals(3, progress?.lives)
        assertEquals(50, progress?.coins)
        assertEquals("normal", progress?.difficulty)
    }

    @Test
    fun `getProgress should return null for non-existing user or game mode`() {
        // When
        val progress = progressRepository.getProgress(testUserId, "nonexistent")

        // Then
        assertNull(progress)
    }
}