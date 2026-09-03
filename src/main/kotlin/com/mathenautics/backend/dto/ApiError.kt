package com.mathenautics.backend.dto

data class ApiError(
    val status: Int,
    val code: String,
    val message: String,
    val timestamp: String
)
