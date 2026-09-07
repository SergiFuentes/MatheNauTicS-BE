package com.mathenautics.backend.dto

import java.util.UUID

data class UserCreateResponse(
    val userId: UUID,
    val username: String,
    val email: String,
    val isGuest: Boolean,
    val token: String
)