package com.mathenautics.backend.domain.repository

import com.mathenautics.backend.dto.GameSessionResult
import com.mathenautics.backend.dto.LeaderboardEntry
import java.util.UUID

interface GameSessionRepository {

    fun saveGameSession(
        userId: UUID,
        gameMode: String,
        score: Int,
        coinsEarned: Int,
        durationSeconds: Int
    ): GameSessionResult

    fun getCurrentCoins(userId: UUID): Int

    fun getLeaderboard(limit: Int, offset: Int, gameMode: String? = null): List<LeaderboardEntry>
}