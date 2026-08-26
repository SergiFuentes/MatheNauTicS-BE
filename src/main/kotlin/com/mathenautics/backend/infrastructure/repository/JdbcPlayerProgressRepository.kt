package com.mathenautics.backend.infrastructure.repository

import com.mathenautics.backend.domain.repository.PlayerProgressRepository
import com.mathenautics.backend.dto.PlayerProgressResponse
import com.mathenautics.backend.util.toOffsetDateTime
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcPlayerProgressRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : PlayerProgressRepository {

    override fun getProgress(userId: UUID): PlayerProgressResponse? {
        val sql = """
            SELECT current_level, last_played_at
            FROM player_progress
            WHERE user_id = :userId
        """
        val params = MapSqlParameterSource().addValue("userId", userId)

        return jdbcTemplate.query(sql, params) { rs, _ ->
            PlayerProgressResponse(
                userId = userId,
                currentLevel = rs.getInt("current_level"),
                lastPlayedAt = rs.getObject("last_played_at")?.toOffsetDateTime()
                    ?: throw IllegalStateException("last_played_at cannot be null")
            )
        }.singleOrNull()
    }

    override fun saveOrUpdate(userId: UUID, level: Int): PlayerProgressResponse {
        val sql = """
            INSERT INTO player_progress (user_id, current_level, last_played_at)
            VALUES (:userId, :level, now())
            ON CONFLICT (user_id) DO UPDATE
            SET current_level = EXCLUDED.current_level,
                last_played_at = now()
            RETURNING current_level, last_played_at
        """
        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("level", level)

        return jdbcTemplate.query(sql, params) { rs, _ ->
            PlayerProgressResponse(
                userId = userId,
                currentLevel = rs.getInt("current_level"),
                lastPlayedAt = rs.getObject("last_played_at")?.toOffsetDateTime()
                    ?: throw IllegalStateException("last_played_at cannot be null")
            )
        }.single()
    }
}