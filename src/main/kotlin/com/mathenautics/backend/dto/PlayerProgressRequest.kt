package com.mathenautics.backend.dto

data class PlayerProgressRequest(
    val gameMode: String,
    val currentLevel: Int,
    val score: Int = 0,
    val lives: Int = 3,
    val coins: Int = 0,
    val difficulty: String = "normal"
)