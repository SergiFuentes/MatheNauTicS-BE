package com.mathenautics.backend.application.service

import com.mathenautics.backend.dto.LoginRequest
import com.mathenautics.backend.dto.LoginResponse

interface AuthService {
    fun login(request: LoginRequest): LoginResponse
}
