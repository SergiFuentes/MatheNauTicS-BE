package com.mathenautics.backend.domain.repository

import com.mathenautics.backend.dto.PlayerProgressResponse
import java.util.UUID

/**
 * Repository interface for player progress data access.
 */
interface PlayerProgressRepository {

    /**
     * Retrieves the progress for a given user and game mode.
     * @return the progress response, or null if not found
     */
    fun getProgress(userId: UUID, gameMode: String): PlayerProgressResponse?

    /**
     * Inserts or updates the player's progress.
     * @param userId the user's UUID
     * @param gameMode the game mode (adventure/training)
     * @param level the current level to save
     * @param score the current score (default 0)
     * @param lives the current lives (default 3)
     * @param coins the current coins (default 0)
     * @param difficulty the current difficulty (default "normal")
     * @return the updated progress response
     */
    fun saveOrUpdate(
        userId: UUID,
        gameMode: String,
        level: Int,
        score: Int = 0,
        lives: Int = 3,
        coins: Int = 0,
        difficulty: String = "normal"
    ): PlayerProgressResponse
}