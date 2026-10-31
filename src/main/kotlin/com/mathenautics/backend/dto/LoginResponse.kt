package com.mathenautics.backend.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class LoginResponse(
    val userId: UUID,
    val username: String,
    val email: String,
    @param:JsonProperty("isGuest")
    @get:JsonProperty("isGuest")
    val isGuest: Boolean = false,
    val token: String
)
