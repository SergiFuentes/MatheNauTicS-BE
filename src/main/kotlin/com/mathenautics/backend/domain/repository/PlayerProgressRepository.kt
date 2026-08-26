package com.mathenautics.backend.domain.repository

import com.mathenautics.backend.dto.PlayerProgressResponse
import java.util.UUID

/**
 * Repository interface for player progress data access.
 */
interface PlayerProgressRepository {

    /**
     * Retrieves the progress for a given user.
     * @return the progress response, or null if not found
     */
    fun getProgress(userId: UUID): PlayerProgressResponse?

    /**
     * Inserts or updates the player's progress.
     * @return the updated progress response
     */
    fun saveOrUpdate(userId: UUID, level: Int): PlayerProgressResponse
}