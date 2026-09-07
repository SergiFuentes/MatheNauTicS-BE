package com.mathenautics.backend.application.service

import com.mathenautics.backend.dto.*
import java.util.UUID

interface GameSessionService {

    fun finishGame(userId: UUID, request: GameResultRequest): GameResultResponse

    fun getCurrentCoins(userId: UUID): PlayerCoinsResponse

    fun getLeaderboard(
        limit: Int,
        offset: Int,
        gameMode: String? = null
    ): List<LeaderboardEntry>

    fun getPlayerProgress(
        userId: UUID,
        gameMode: String
    ): PlayerProgressResponse

    fun updatePlayerProgress(
        userId: UUID,
        request: PlayerProgressRequest
    ): PlayerProgressResponse
}