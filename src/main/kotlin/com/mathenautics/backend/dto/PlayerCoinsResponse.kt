package com.mathenautics.backend.dto

import java.util.UUID

data class PlayerCoinsResponse(
    val userId: UUID,
    val totalCoins: Int
)