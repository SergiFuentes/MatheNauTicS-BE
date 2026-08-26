package com.mathenautics.backend.dto

import java.time.OffsetDateTime

data class LeaderboardEntry(
    val username: String,
    val score: Int,
    val totalCoins: Int,
    val createdAt: OffsetDateTime
)