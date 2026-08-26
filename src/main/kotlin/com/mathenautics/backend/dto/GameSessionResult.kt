package com.mathenautics.backend.dto

/**
 * Internal result object returned by the repository layer.
 * Contains the raw data from the database after saving a game session.
 * This is then mapped to GameResultResponse in the service layer.
 */
data class GameSessionResult(
    val totalCoins: Int,
    val sessionId: Long
)