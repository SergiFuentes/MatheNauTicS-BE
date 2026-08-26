package com.mathenautics.backend.dto

data class UserCreateRequest(
    val username: String,
    val email: String,
    val password: String,
    val isGuest: Boolean = true
)