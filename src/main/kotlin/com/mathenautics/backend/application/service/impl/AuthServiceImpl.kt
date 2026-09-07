package com.mathenautics.backend.application.service.impl

import com.mathenautics.backend.application.exception.InvalidCredentialsException
import com.mathenautics.backend.application.service.AuthService
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.LoginRequest
import com.mathenautics.backend.dto.LoginResponse
import com.mathenautics.backend.security.JwtService
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Service

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) : AuthService {

    override fun login(request: LoginRequest): LoginResponse {
        val identifier = request.identifier.trim()
        require(identifier.isNotEmpty()) { "Username or email is required" }
        require(request.password.isNotEmpty()) { "Password is required" }

        val credentials = userRepository.findCredentialsByIdentifier(identifier)
            ?: throw InvalidCredentialsException()

        if (!BCrypt.checkpw(request.password, credentials.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val token = jwtService.generateToken(
            userId = credentials.id,
            username = credentials.username,
            isGuest = credentials.isGuest
        )

        return LoginResponse(
            userId = credentials.id,
            username = credentials.username,
            email = credentials.email,
            isGuest = credentials.isGuest,
            token = token
        )
    }
}