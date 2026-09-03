package com.mathenautics.backend.dto

data class LoginRequest(
    val identifier: String,
    val password: String
)
