package com.mathenautics.backend.domain.repository

import com.mathenautics.backend.dto.GameSessionResult
import com.mathenautics.backend.dto.LeaderboardEntry
import java.time.OffsetDateTime
import java.util.UUID

interface GameSessionRepository {

    fun createGameSessionTicket(
        userId: UUID,
        gameMode: String,
        sessionToken: UUID,
        expiresAt: OffsetDateTime
    )

    fun saveGameSession(
        userId: UUID,
        gameMode: String,
        score: Int,
        coinsEarned: Int,
        durationSeconds: Int,
        sessionToken: UUID
    ): GameSessionResult

    fun getCurrentCoins(userId: UUID): Int

    fun getLeaderboard(limit: Int, offset: Int, gameMode: String? = null): List<LeaderboardEntry>
}