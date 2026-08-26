package com.mathenautics.backend.dto

import java.util.UUID

data class GameResultResponse(
    val sessionId: Long,
    val userId: UUID,
    val score: Int,
    val totalCoins: Int
)