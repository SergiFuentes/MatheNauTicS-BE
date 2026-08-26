package com.mathenautics.backend.dto

import java.time.OffsetDateTime
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val createdAt: OffsetDateTime,
    val isGuest: Boolean = true
)