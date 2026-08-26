package com.mathenautics.backend.dto

import java.util.UUID

data class PlayerProgressRequest(
    val userId: UUID,
    val currentLevel: Int
)