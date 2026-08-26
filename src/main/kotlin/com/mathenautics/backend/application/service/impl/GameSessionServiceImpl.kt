package com.mathenautics.backend.application.service.impl

import com.mathenautics.backend.application.service.GameSessionService
import com.mathenautics.backend.domain.repository.GameSessionRepository
import com.mathenautics.backend.domain.repository.PlayerProgressRepository
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Implementation of GameSessionService.
 */
@Service
class GameSessionServiceImpl(
    private val gameSessionRepository: GameSessionRepository,
    private val userRepository: UserRepository,
    private val progressRepository: PlayerProgressRepository
) : GameSessionService {

    @Transactional
    override fun finishGame(request: GameResultRequest): GameResultResponse {
        // Validations
        if (request.score < 0) {
            throw IllegalArgumentException("Score cannot be negative")
        }
        if (request.coinsEarned < 0) {
            throw IllegalArgumentException("Coins earned cannot be negative")
        }
        if (request.durationSeconds < 0) {
            throw IllegalArgumentException("Duration cannot be negative")
        }

        if (!userRepository.existsById(request.userId)) {
            throw IllegalArgumentException("User does not exist")
        }

        val result = gameSessionRepository.saveGameSession(
            userId = request.userId,
            gameMode = request.gameMode,
            score = request.score,
            coinsEarned = request.coinsEarned,
            durationSeconds = request.durationSeconds
        )

        return GameResultResponse(
            sessionId = result.sessionId,
            userId = request.userId,
            score = request.score,
            totalCoins = result.totalCoins
        )
    }

    override fun getCurrentCoins(userId: UUID): PlayerCoinsResponse {
        val total = gameSessionRepository.getCurrentCoins(userId)
        return PlayerCoinsResponse(
            userId = userId,
            totalCoins = total
        )
    }

    override fun getLeaderboard(limit: Int, offset: Int, gameMode: String?): List<LeaderboardEntry> {
        return gameSessionRepository.getLeaderboard(limit, offset, gameMode)
    }

    @Transactional
    override fun getPlayerProgress(userId: UUID): PlayerProgressResponse {
        return progressRepository.getProgress(userId)
            ?: PlayerProgressResponse(
                userId = userId,
                currentLevel = 1,
                lastPlayedAt = java.time.OffsetDateTime.now()
            )
    }

    @Transactional
    override fun updatePlayerProgress(request: PlayerProgressRequest): PlayerProgressResponse {
        if (request.currentLevel < 1) {
            throw IllegalArgumentException("Level must be at least 1")
        }
        return progressRepository.saveOrUpdate(request.userId, request.currentLevel)
    }
}