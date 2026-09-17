package com.mathenautics.backend.application.service.impl

import com.mathenautics.backend.application.service.GameSessionService
import com.mathenautics.backend.domain.repository.GameSessionRepository
import com.mathenautics.backend.domain.repository.PlayerProgressRepository
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class GameSessionServiceImpl(
    private val gameSessionRepository: GameSessionRepository,
    private val userRepository: UserRepository,
    private val progressRepository: PlayerProgressRepository
) : GameSessionService {

    companion object {
        private const val MAX_SCORE = 1_000_000
        private const val MAX_COINS_PER_SESSION = 10_000
        private const val MAX_DURATION_SECONDS = 3_600
        private const val MAX_LEVEL = 13
        private val VALID_GAME_MODES = setOf("adventure", "training")
        private val VALID_DIFFICULTIES = setOf("easy", "normal", "pro")
    }

    @Transactional
    override fun finishGame(userId: UUID, request: GameResultRequest): GameResultResponse {
        if (request.score < 0) throw IllegalArgumentException("Score cannot be negative")
        if (request.score > MAX_SCORE) throw IllegalArgumentException("Score exceeds the maximum allowed value")
        if (request.coinsEarned < 0) throw IllegalArgumentException("Coins earned cannot be negative")
        if (request.coinsEarned > MAX_COINS_PER_SESSION) {
            throw IllegalArgumentException("Coins earned exceed the maximum allowed value")
        }
        if (request.durationSeconds < 0) throw IllegalArgumentException("Duration cannot be negative")
        if (request.durationSeconds > MAX_DURATION_SECONDS) {
            throw IllegalArgumentException("Duration exceeds the maximum allowed value")
        }
        if (request.gameMode !in VALID_GAME_MODES) throw IllegalArgumentException("Invalid game mode")

        if (!userRepository.existsById(userId)) {
            throw IllegalArgumentException("User does not exist")
        }

        val result = gameSessionRepository.saveGameSession(
            userId = userId,
            gameMode = request.gameMode,
            score = request.score,
            coinsEarned = request.coinsEarned,
            durationSeconds = request.durationSeconds,
            sessionToken = request.sessionToken
        )

        return GameResultResponse(
            sessionId = result.sessionId,
            userId = userId,
            score = request.score,
            totalCoins = result.totalCoins
        )
    }

    override fun getCurrentCoins(userId: UUID): PlayerCoinsResponse {
        val total = gameSessionRepository.getCurrentCoins(userId)
        return PlayerCoinsResponse(userId = userId, totalCoins = total)
    }

    override fun getLeaderboard(
        limit: Int,
        offset: Int,
        gameMode: String?
    ): List<LeaderboardEntry> {
        return gameSessionRepository.getLeaderboard(limit, offset, gameMode)
    }

    override fun getPlayerProgress(
        userId: UUID,
        gameMode: String
    ): PlayerProgressResponse {
        return progressRepository.getProgress(userId, gameMode)
            ?: PlayerProgressResponse(
                userId = userId,
                gameMode = gameMode,
                currentLevel = 1,
                score = 0,
                lives = 3,
                coins = 0,
                difficulty = "normal",
                lastPlayedAt = OffsetDateTime.now()
            )
    }

    @Transactional
    override fun updatePlayerProgress(
        userId: UUID,
        request: PlayerProgressRequest
    ): PlayerProgressResponse {
        if (request.currentLevel < 1) throw IllegalArgumentException("Level must be at least 1")
        if (request.currentLevel > MAX_LEVEL) throw IllegalArgumentException("Level exceeds the maximum allowed value")
        if (request.gameMode !in VALID_GAME_MODES) throw IllegalArgumentException("Invalid game mode")
        if (request.difficulty !in VALID_DIFFICULTIES) throw IllegalArgumentException("Invalid difficulty")

        return progressRepository.saveOrUpdate(
            userId = userId,
            gameMode = request.gameMode,
            level = request.currentLevel,
            score = request.score,
            lives = request.lives,
            coins = request.coins,
            difficulty = request.difficulty
        )
    }
}