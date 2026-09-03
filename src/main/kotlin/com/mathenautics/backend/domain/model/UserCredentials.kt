package com.mathenautics.backend.domain.model

import java.util.UUID

data class UserCredentials(
    val id: UUID,
    val username: String,
    val email: String,
    val passwordHash: String,
    val isGuest: Boolean
)
