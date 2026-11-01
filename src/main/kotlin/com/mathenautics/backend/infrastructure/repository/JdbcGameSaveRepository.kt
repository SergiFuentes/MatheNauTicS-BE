package com.mathenautics.backend.infrastructure.repository

import com.mathenautics.backend.application.exception.GameSaveLimitExceededException
import com.mathenautics.backend.domain.repository.GameSaveRepository
import com.mathenautics.backend.dto.GameSaveResponse
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class JdbcGameSaveRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val jsonMapper: JsonMapper
) : GameSaveRepository {

    override fun findAllByUserId(userId: UUID): List<GameSaveResponse> {
        val sql = """
            SELECT id, user_id, game_mode, name, state, created_at, updated_at
            FROM game_saves
            WHERE user_id = :userId
            ORDER BY updated_at DESC, id
        """
        return jdbcTemplate.query(sql, params(userId)) { rs, _ -> mapSave(
            rs.getObject("id", UUID::class.java),
            rs.getObject("user_id", UUID::class.java),
            rs.getString("game_mode"),
            rs.getString("name"),
            rs.getString("state"),
            rs.getObject("created_at", OffsetDateTime::class.java),
            rs.getObject("updated_at", OffsetDateTime::class.java)
        ) }
    }

    override fun findById(userId: UUID, saveId: UUID): GameSaveResponse? {
        val sql = """
            SELECT id, user_id, game_mode, name, state, created_at, updated_at
            FROM game_saves
            WHERE user_id = :userId AND id = :saveId
        """
        return jdbcTemplate.query(sql, params(userId).addValue("saveId", saveId)) { rs, _ -> mapSave(
            rs.getObject("id", UUID::class.java),
            rs.getObject("user_id", UUID::class.java),
            rs.getString("game_mode"),
            rs.getString("name"),
            rs.getString("state"),
            rs.getObject("created_at", OffsetDateTime::class.java),
            rs.getObject("updated_at", OffsetDateTime::class.java)
        ) }.singleOrNull()
    }

    @Transactional
    override fun create(
        userId: UUID,
        gameMode: String,
        name: String,
        state: JsonNode
    ): GameSaveResponse {
        // Serializes slot creation for a user so concurrent requests cannot exceed the limit.
        val userExists = jdbcTemplate.query(
            "SELECT id FROM users WHERE id = :userId FOR UPDATE",
            params(userId)
        ) { rs, _ -> rs.getObject("id", UUID::class.java) }.isNotEmpty()
        if (!userExists) throw IllegalArgumentException("User does not exist")

        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM game_saves WHERE user_id = :userId",
            params(userId),
            Int::class.java
        ) ?: 0
        if (count >= MAX_SAVES_PER_USER) throw GameSaveLimitExceededException()

        val sql = """
            INSERT INTO game_saves (user_id, game_mode, name, state)
            VALUES (:userId, :gameMode, :name, CAST(:state AS jsonb))
            RETURNING id, user_id, game_mode, name, state, created_at, updated_at
        """
        val insertParams = params(userId)
            .addValue("gameMode", gameMode)
            .addValue("name", name)
            .addValue("state", jsonMapper.writeValueAsString(state))
        return jdbcTemplate.query(sql, insertParams) { rs, _ -> mapSave(
            rs.getObject("id", UUID::class.java),
            rs.getObject("user_id", UUID::class.java),
            rs.getString("game_mode"),
            rs.getString("name"),
            rs.getString("state"),
            rs.getObject("created_at", OffsetDateTime::class.java),
            rs.getObject("updated_at", OffsetDateTime::class.java)
        ) }.single()
    }

    override fun update(
        userId: UUID,
        saveId: UUID,
        name: String,
        state: JsonNode
    ): GameSaveResponse? {
        val sql = """
            UPDATE game_saves
            SET name = :name, state = CAST(:state AS jsonb), updated_at = now()
            WHERE user_id = :userId AND id = :saveId
            RETURNING id, user_id, game_mode, name, state, created_at, updated_at
        """
        val updateParams = params(userId)
            .addValue("saveId", saveId)
            .addValue("name", name)
            .addValue("state", jsonMapper.writeValueAsString(state))
        return jdbcTemplate.query(sql, updateParams) { rs, _ -> mapSave(
            rs.getObject("id", UUID::class.java),
            rs.getObject("user_id", UUID::class.java),
            rs.getString("game_mode"),
            rs.getString("name"),
            rs.getString("state"),
            rs.getObject("created_at", OffsetDateTime::class.java),
            rs.getObject("updated_at", OffsetDateTime::class.java)
        ) }.singleOrNull()
    }

    override fun delete(userId: UUID, saveId: UUID): Boolean {
        val sql = "DELETE FROM game_saves WHERE user_id = :userId AND id = :saveId"
        return jdbcTemplate.update(sql, params(userId).addValue("saveId", saveId)) == 1
    }

    private fun params(userId: UUID) = MapSqlParameterSource().addValue("userId", userId)

    private fun mapSave(
        id: UUID,
        userId: UUID,
        gameMode: String,
        name: String,
        stateJson: String,
        createdAt: OffsetDateTime,
        updatedAt: OffsetDateTime
    ) = GameSaveResponse(
        id = id,
        userId = userId,
        gameMode = gameMode,
        name = name,
        state = jsonMapper.readTree(stateJson),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        private const val MAX_SAVES_PER_USER = 10
    }
}
