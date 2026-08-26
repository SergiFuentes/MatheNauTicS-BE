package com.mathenautics.backend.model

import java.time.OffsetDateTime
import java.util.UUID

data class User(
    val id: UUID,
    val username: String,
    val email: String,
    val passwordHash: String,
    val createdAt: OffsetDateTime
)