package com.mathenautics.backend.integration.repository

import com.mathenautics.backend.application.exception.GameSaveLimitExceededException
import com.mathenautics.backend.infrastructure.repository.JdbcGameSaveRepository
import com.mathenautics.backend.integration.IntegrationTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class JdbcGameSaveRepositoryIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var gameSaveRepository: JdbcGameSaveRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    private val testUserId = UUID.randomUUID()

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM game_saves")
        jdbcTemplate.execute("DELETE FROM users")
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, email, password_hash, is_guest, created_at)
            VALUES (?, 'saveuser', 'saveuser@example.com', 'hash', false, NOW())
            """,
            testUserId
        )
    }

    @Test
    fun `create should persist a save and list it for its owner`() {
        val state = jsonMapper.readTree("""{"level":3,"score":120,"lives":2}""")
        val saved = gameSaveRepository.create(testUserId, "adventure", "Castle", state)

        assertEquals(testUserId, saved.userId)
        assertEquals("adventure", saved.gameMode)
        assertEquals("Castle", saved.name)
        assertEquals(3, saved.state["level"].intValue())
        assertEquals(1, gameSaveRepository.findAllByUserId(testUserId).size)
        assertNull(gameSaveRepository.findById(UUID.randomUUID(), saved.id))
    }

    @Test
    fun `update should change name and state without creating another slot`() {
        val first = gameSaveRepository.create(
            testUserId, "adventure", "First", jsonMapper.readTree("""{"level":1}""")
        )

        val updated = gameSaveRepository.update(
            testUserId, first.id, "Renamed", jsonMapper.readTree("""{"level":4}""")
        )

        assertNotNull(updated)
        assertEquals("Renamed", updated?.name)
        assertEquals(4, updated?.state?.get("level")?.intValue())
        assertEquals(1, gameSaveRepository.findAllByUserId(testUserId).size)
    }

    @Test
    fun `delete should only remove a save owned by the user`() {
        val saved = gameSaveRepository.create(
            testUserId, "training", "Practice", jsonMapper.readTree("""{"table":7}""")
        )

        assertFalse(gameSaveRepository.delete(UUID.randomUUID(), saved.id))
        assertTrue(gameSaveRepository.delete(testUserId, saved.id))
        assertTrue(gameSaveRepository.findAllByUserId(testUserId).isEmpty())
    }

    @Test
    fun `create should enforce the ten save limit`() {
        repeat(10) { index ->
            gameSaveRepository.create(
                testUserId,
                if (index % 2 == 0) "adventure" else "training",
                "Save $index",
                jsonMapper.readTree("""{"slot":$index}""")
            )
        }

        assertThrows(GameSaveLimitExceededException::class.java) {
            gameSaveRepository.create(
                testUserId, "adventure", "Too many", jsonMapper.readTree("""{"slot":10}""")
            )
        }
        assertEquals(10, gameSaveRepository.findAllByUserId(testUserId).size)
    }
}
