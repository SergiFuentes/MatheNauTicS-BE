package com.mathenautics.backend.dto

import java.util.UUID

data class PlayerProgressRequest(
    val userId: UUID,
    val gameMode: String,
    val currentLevel: Int,
    val score: Int = 0,
    val lives: Int = 3,
    val coins: Int = 0,
    val difficulty: String = "normal"
)