package com.mathenautics.backend.dto

import java.util.UUID

data class GameResultRequest(
    val gameMode: String,
    val score: Int,
    val coinsEarned: Int,
    val durationSeconds: Int,
    val sessionToken: UUID
)