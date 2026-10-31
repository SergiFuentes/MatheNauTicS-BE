package com.mathenautics.backend.dto

import tools.jackson.annotation.JsonProperty
import java.time.OffsetDateTime
import java.util.UUID

data class UserUpdateResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val createdAt: OffsetDateTime,
    @param:JsonProperty("isGuest")
    @get:JsonProperty("isGuest")
    val isGuest: Boolean,
    val token: String
)
