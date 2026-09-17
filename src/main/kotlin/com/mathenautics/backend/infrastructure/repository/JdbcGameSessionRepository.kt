package com.mathenautics.backend.infrastructure.repository

import com.mathenautics.backend.application.exception.DuplicateGameSessionException
import com.mathenautics.backend.domain.repository.GameSessionRepository
import com.mathenautics.backend.dto.GameSessionResult
import com.mathenautics.backend.dto.LeaderboardEntry
import com.mathenautics.backend.util.toOffsetDateTime
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcGameSessionRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : GameSessionRepository {

    private data class ExistingSession(
        val sessionId: Long,
        val userId: UUID
    )

    override fun saveGameSession(
        userId: UUID,
        gameMode: String,
        score: Int,
        coinsEarned: Int,
        durationSeconds: Int,
        sessionToken: UUID
    ): GameSessionResult {
        val existing = findSessionByToken(sessionToken)
        if (existing != null) {
            if (existing.userId != userId) {
                throw DuplicateGameSessionException("Session token already used by another user")
            }
            return GameSessionResult(
                totalCoins = getCurrentCoins(userId),
                sessionId = existing.sessionId
            )
        }

        val insertSql = """
            INSERT INTO game_sessions (user_id, game_mode, score, total_coins, duration_seconds, session_token)
            VALUES (:userId, :gameMode, :score, :totalCoins, :durationSeconds, :sessionToken)
            RETURNING id
        """

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("gameMode", gameMode)
            .addValue("score", score)
            .addValue("totalCoins", coinsEarned)
            .addValue("durationSeconds", durationSeconds)
            .addValue("sessionToken", sessionToken)

        val sessionId = try {
            jdbcTemplate.queryForObject(insertSql, params) { rs, _ ->
                rs.getLong("id")
            }!!
        } catch (e: DataIntegrityViolationException) {
            // Concurrent request with the same token won the race.
            val raced = findSessionByToken(sessionToken)
            if (raced != null && raced.userId == userId) {
                return GameSessionResult(
                    totalCoins = getCurrentCoins(userId),
                    sessionId = raced.sessionId
                )
            }
            throw e
        }

        val newTotal = getCurrentCoins(userId)

        return GameSessionResult(
            totalCoins = newTotal,
            sessionId = sessionId
        )
    }

    override fun getCurrentCoins(userId: UUID): Int {
        val sql = """
        SELECT COALESCE(SUM(total_coins), 0)
        FROM game_sessions
        WHERE user_id = :userId
    """
        val params = MapSqlParameterSource().addValue("userId", userId)

        return jdbcTemplate.queryForObject(sql, params, Int::class.java) ?: 0
    }

    override fun getLeaderboard(limit: Int, offset: Int, gameMode: String?): List<LeaderboardEntry> {
        val sql = if (gameMode == null) {
            """
                SELECT username, game_mode, score, total_coins, created_at
                FROM leaderboard
                ORDER BY score DESC
                LIMIT :limit OFFSET :offset
            """
        } else {
            """
                SELECT username, game_mode, score, total_coins, created_at
                FROM leaderboard
                WHERE game_mode = :gameMode
                ORDER BY score DESC
                LIMIT :limit OFFSET :offset
            """
        }

        val params = MapSqlParameterSource()
            .addValue("limit", limit)
            .addValue("offset", offset)
        if (gameMode != null) {
            params.addValue("gameMode", gameMode)
        }

        return jdbcTemplate.query(sql, params) { rs, _ ->
            LeaderboardEntry(
                username = rs.getString("username"),
                gameMode = rs.getString("game_mode"),
                score = rs.getInt("score"),
                totalCoins = rs.getInt("total_coins"),
                createdAt = rs.getObject("created_at")?.toOffsetDateTime()
                    ?: throw IllegalStateException("created_at cannot be null")
            )
        }
    }

    private fun findSessionByToken(token: UUID): ExistingSession? {
        val sql = "SELECT id, user_id FROM game_sessions WHERE session_token = :token"
        return jdbcTemplate.query(
            sql,
            MapSqlParameterSource("token", token)
        ) { rs, _ ->
            ExistingSession(
                sessionId = rs.getLong("id"),
                userId = rs.getObject("user_id", UUID::class.java)
            )
        }.singleOrNull()
    }
}