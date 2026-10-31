package com.mathenautics.backend.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UserCreateRequest(
    val username: String,
    val email: String,
    val password: String,
    @param:JsonProperty("isGuest")
    @get:JsonProperty("isGuest")
    val isGuest: Boolean = true
)
