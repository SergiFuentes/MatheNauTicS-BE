package com.mathenautics.backend.infrastructure.repository

import com.mathenautics.backend.domain.repository.GameSessionRepository
import com.mathenautics.backend.dto.GameSessionResult
import com.mathenautics.backend.dto.LeaderboardEntry
import com.mathenautics.backend.util.toOffsetDateTime
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcGameSessionRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : GameSessionRepository {

    override fun saveGameSession(
        userId: UUID,
        gameMode: String,
        score: Int,
        coinsEarned: Int,
        durationSeconds: Int
    ): GameSessionResult {
        val currentCoins = getCurrentCoins(userId)
        val newTotal = currentCoins + coinsEarned

        val insertSql = """
            INSERT INTO game_sessions (user_id, game_mode, score, total_coins, duration_seconds)
            VALUES (:userId, :gameMode, :score, :totalCoins, :durationSeconds)
            RETURNING id
        """

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("gameMode", gameMode)
            .addValue("score", score)
            .addValue("totalCoins", newTotal)
            .addValue("durationSeconds", durationSeconds)

        val sessionId = jdbcTemplate.queryForObject(insertSql, params) { rs, _ ->
            rs.getLong("id")
        }!!

        return GameSessionResult(
            totalCoins = newTotal,
            sessionId = sessionId
        )
    }

    override fun getCurrentCoins(userId: UUID): Int {
        val sql = """
            SELECT total_coins
            FROM game_sessions
            WHERE user_id = :userId
            ORDER BY created_at DESC
            LIMIT 1
        """
        val params = MapSqlParameterSource().addValue("userId", userId)

        return try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ ->
                rs.getInt("total_coins")
            } ?: 0
        } catch (e: EmptyResultDataAccessException) {
            0
        }
    }

    override fun getLeaderboard(limit: Int, offset: Int, gameMode: String?): List<LeaderboardEntry> {
        val sql = """
            SELECT username, game_mode, score, total_coins, created_at
            FROM leaderboard
            WHERE (:gameMode IS NULL OR game_mode = :gameMode)
            ORDER BY score DESC
            LIMIT :limit OFFSET :offset
        """
        val params = MapSqlParameterSource()
            .addValue("limit", limit)
            .addValue("offset", offset)
            .addValue("gameMode", gameMode)

        return jdbcTemplate.query(sql, params) { rs, _ ->
            LeaderboardEntry(
                username = rs.getString("username"),
                score = rs.getInt("score"),
                totalCoins = rs.getInt("total_coins"),
                createdAt = rs.getObject("created_at")?.toOffsetDateTime()
                    ?: throw IllegalStateException("created_at cannot be null")
            )
        }
    }
}