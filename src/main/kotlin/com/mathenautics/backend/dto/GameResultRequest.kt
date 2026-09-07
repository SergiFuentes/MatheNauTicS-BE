package com.mathenautics.backend.dto

data class GameResultRequest(
    val gameMode: String,
    val score: Int,
    val coinsEarned: Int,
    val durationSeconds: Int
)