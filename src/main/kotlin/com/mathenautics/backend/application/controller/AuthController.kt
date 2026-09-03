package com.mathenautics.backend.application.controller

import com.mathenautics.backend.application.service.AuthService
import com.mathenautics.backend.dto.LoginRequest
import com.mathenautics.backend.dto.LoginResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(authService.login(request))
}
