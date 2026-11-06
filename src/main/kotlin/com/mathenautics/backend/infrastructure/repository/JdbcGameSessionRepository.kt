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
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class JdbcGameSessionRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : GameSessionRepository {

    private data class ExistingSession(
        val sessionId: Long,
        val userId: UUID,
        val gameMode: String,
        val score: Int,
        val durationSeconds: Int
    )

    override fun createGameSessionTicket(
        userId: UUID,
        gameMode: String,
        sessionToken: UUID,
        expiresAt: OffsetDateTime
    ) {
        val sql = """
            INSERT INTO game_session_tickets (session_token, user_id, game_mode, expires_at)
            VALUES (:sessionToken, :userId, :gameMode, :expiresAt)
        """
        val params = MapSqlParameterSource()
            .addValue("sessionToken", sessionToken)
            .addValue("userId", userId)
            .addValue("gameMode", gameMode)
            .addValue("expiresAt", expiresAt)
        jdbcTemplate.update(sql, params)
    }

    override fun saveGameSession(
        userId: UUID,
        gameMode: String,
        score: Int,
        coinsEarned: Int,
        durationSeconds: Int,
        sessionToken: UUID
    ): GameSessionResult {
        // Completed sessions remain idempotent: retries return the original result,
        // including after the ticket expires or has already been consumed.
        val existing = findSessionByToken(sessionToken)
        if (existing != null) {
            if (existing.userId != userId) {
                throw DuplicateGameSessionException("Session token already used by another user")
            }
            ensureSamePayload(existing, gameMode, score, durationSeconds)
            return GameSessionResult(
                totalCoins = getCurrentCoins(userId),
                sessionId = existing.sessionId
            )
        }

        // Lock the server-issued ticket so concurrent finish requests cannot both
        // consume it. Recheck for a completed session after acquiring the lock.
        val ticketSql = """
            SELECT user_id, game_mode, expires_at, finished_at
            FROM game_session_tickets
            WHERE session_token = :sessionToken
            FOR UPDATE
        """
        val ticket = jdbcTemplate.query(
            ticketSql,
            MapSqlParameterSource("sessionToken", sessionToken)
        ) { rs, _ ->
            mapOf(
                "userId" to rs.getObject("user_id", UUID::class.java),
                "gameMode" to rs.getString("game_mode"),
                "expiresAt" to rs.getObject("expires_at", OffsetDateTime::class.java),
                "finishedAt" to rs.getObject("finished_at", OffsetDateTime::class.java)
            )
        }.singleOrNull() ?: throw IllegalArgumentException("Invalid game session ticket")

        if (ticket["userId"] != userId || ticket["gameMode"] != gameMode) {
            throw IllegalArgumentException("Game session ticket does not match this user or game mode")
        }

        val racedSession = findSessionByToken(sessionToken)
        if (racedSession != null) {
            if (racedSession.userId != userId) {
                throw DuplicateGameSessionException("Session token already used by another user")
            }
            ensureSamePayload(racedSession, gameMode, score, durationSeconds)
            return GameSessionResult(
                totalCoins = getCurrentCoins(userId),
                sessionId = racedSession.sessionId
            )
        }

        val expiresAt = ticket["expiresAt"] as OffsetDateTime
        if (!expiresAt.isAfter(OffsetDateTime.now())) {
            throw IllegalArgumentException("Game session ticket has expired")
        }
        if (ticket["finishedAt"] != null) {
            throw IllegalArgumentException("Game session ticket has already been used")
        }

        val consumeSql = """
            UPDATE game_session_tickets
            SET finished_at = CURRENT_TIMESTAMP
            WHERE session_token = :sessionToken AND finished_at IS NULL
        """
        val consumed = jdbcTemplate.update(
            consumeSql,
            MapSqlParameterSource("sessionToken", sessionToken)
        )
        if (consumed != 1) {
            throw IllegalArgumentException("Game session ticket has already been used")
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
                ensureSamePayload(raced, gameMode, score, durationSeconds)
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

    private fun ensureSamePayload(
        existing: ExistingSession,
        gameMode: String,
        score: Int,
        durationSeconds: Int
    ) {
        if (
            existing.gameMode != gameMode ||
            existing.score != score ||
            existing.durationSeconds != durationSeconds
        ) {
            throw DuplicateGameSessionException(
                "Session token already used with a different game result"
            )
        }
    }

    private fun findSessionByToken(token: UUID): ExistingSession? {
        val sql = """
            SELECT id, user_id, game_mode, score, duration_seconds
            FROM game_sessions
            WHERE session_token = :token
        """
        return jdbcTemplate.query(
            sql,
            MapSqlParameterSource("token", token)
        ) { rs, _ ->
            ExistingSession(
                sessionId = rs.getLong("id"),
                userId = rs.getObject("user_id", UUID::class.java),
                gameMode = rs.getString("game_mode"),
                score = rs.getInt("score"),
                durationSeconds = rs.getInt("duration_seconds")
            )
        }.singleOrNull()
    }
}