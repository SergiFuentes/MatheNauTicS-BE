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

    override fun getProgress(
        userId: UUID,
        gameMode: String
    ): PlayerProgressResponse? {
        val sql = """
        SELECT user_id, game_mode, current_level, score, lives, coins, difficulty, last_played_at
        FROM player_progress
        WHERE user_id = :userId
          AND game_mode = :gameMode
        """

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("gameMode", gameMode)

        return jdbcTemplate.query(sql, params) { rs, _ ->
            PlayerProgressResponse(
                userId = rs.getObject("user_id", UUID::class.java),
                gameMode = rs.getString("game_mode"),
                currentLevel = rs.getInt("current_level"),
                score = rs.getInt("score"),
                lives = rs.getInt("lives"),
                coins = rs.getInt("coins"),
                difficulty = rs.getString("difficulty"),
                lastPlayedAt = rs.getObject("last_played_at")?.toOffsetDateTime()
                    ?: throw IllegalStateException("last_played_at cannot be null")
            )
        }.singleOrNull()
    }

    override fun saveOrUpdate(
        userId: UUID,
        gameMode: String,
        level: Int,
        score: Int,
        lives: Int,
        coins: Int,
        difficulty: String
    ): PlayerProgressResponse {
        val sql = """
        INSERT INTO player_progress (user_id, game_mode, current_level, last_played_at, score, lives, coins, difficulty)
        VALUES (:userId, :gameMode, :level, now(), :score, :lives, :coins, :difficulty)
        ON CONFLICT (user_id, game_mode) DO UPDATE
        SET current_level = EXCLUDED.current_level,
            last_played_at = now(),
            score = EXCLUDED.score,
            lives = EXCLUDED.lives,
            coins = EXCLUDED.coins,
            difficulty = EXCLUDED.difficulty
        RETURNING user_id, game_mode, current_level, score, lives, coins, difficulty, last_played_at
        """

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("gameMode", gameMode)
            .addValue("level", level)
            .addValue("score", score)
            .addValue("lives", lives)
            .addValue("coins", coins)
            .addValue("difficulty", difficulty)

        return jdbcTemplate.query(sql, params) { rs, _ ->
            PlayerProgressResponse(
                userId = rs.getObject("user_id", UUID::class.java),
                gameMode = rs.getString("game_mode"),
                currentLevel = rs.getInt("current_level"),
                score = rs.getInt("score"),
                lives = rs.getInt("lives"),
                coins = rs.getInt("coins"),
                difficulty = rs.getString("difficulty"),
                lastPlayedAt = rs.getObject("last_played_at")?.toOffsetDateTime()
                    ?: throw IllegalStateException("last_played_at cannot be null")
            )
        }.single()
    }
}