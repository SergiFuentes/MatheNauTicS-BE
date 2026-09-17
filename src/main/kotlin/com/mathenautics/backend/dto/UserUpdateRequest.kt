package com.mathenautics.backend.dto

data class UserUpdateRequest(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val currentPassword: String? = null
)