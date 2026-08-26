package com.mathenautics.backend.dto

import java.time.OffsetDateTime
import java.util.UUID

data class PlayerProgressResponse(
    val userId: UUID,
    val currentLevel: Int,
    val lastPlayedAt: OffsetDateTime
)