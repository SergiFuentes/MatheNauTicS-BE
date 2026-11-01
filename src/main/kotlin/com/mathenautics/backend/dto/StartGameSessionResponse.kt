package com.mathenautics.backend.dto

import java.time.OffsetDateTime
import java.util.UUID

data class StartGameSessionResponse(
    val sessionToken: UUID,
    val gameMode: String,
    val expiresAt: OffsetDateTime
)
