package com.mathenautics.backend.dto

import java.time.OffsetDateTime
import java.util.UUID

data class PlayerProgressResponse(
    val userId: UUID,
    val gameMode: String,
    val currentLevel: Int,
    val score: Int,
    val lives: Int,
    val coins: Int,
    val difficulty: String,
    val lastPlayedAt: OffsetDateTime
)