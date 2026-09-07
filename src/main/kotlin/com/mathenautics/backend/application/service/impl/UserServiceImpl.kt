package com.mathenautics.backend.application.service.impl

import com.mathenautics.backend.application.exception.EmailAlreadyExistsException
import com.mathenautics.backend.application.exception.GuestConversionException
import com.mathenautics.backend.application.exception.UsernameAlreadyExistsException
import com.mathenautics.backend.application.exception.UserNotFoundException
import com.mathenautics.backend.application.service.UserService
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserCreateResponse
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
import com.mathenautics.backend.security.JwtService
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) : UserService {
    @Transactional
    override fun createUser(request: UserCreateRequest): UserCreateResponse {
        validateCredentials(request.username, request.email, request.password)
        ensureUnique(request.username, request.email, null)

        val user = userRepository.create(
            username = request.username.trim(),
            email = request.email.trim().lowercase(),
            passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt()),
            isGuest = request.isGuest
        )

        val token = jwtService.generateToken(
            userId = user.id,
            username = user.username,
            isGuest = user.isGuest
        )

        return UserCreateResponse(
            userId = user.id,
            username = user.username,
            email = user.email,
            isGuest = user.isGuest,
            token = token
        )
    }

    override fun getUser(userId: UUID): UserResponse =
        userRepository.findById(userId) ?: throw UserNotFoundException()

    @Transactional
    override fun updateUser(userId: UUID, request: UserUpdateRequest): UserResponse {
        val current = getUser(userId)
        println("UPDATE REQUEST: username=${request.username}, email=${request.email}, password=${if (request.password != null) "***" else "null"}")

        request.username?.let {
            require(it.isNotBlank()) { "Username is required" }
            if (userRepository.existsByUsername(it.trim(), userId)) throw UsernameAlreadyExistsException()
        }
        request.email?.let {
            require(isValidEmail(it)) { "Valid email is required" }
            if (userRepository.existsByEmail(it.trim(), userId)) throw EmailAlreadyExistsException()
        }
        request.password?.let { require(it.length >= 6) { "Password must be at least 6 characters" } }

        val updated = userRepository.update(
            userId = userId,
            username = request.username?.trim(),
            email = request.email?.trim()?.lowercase(),
            passwordHash = request.password?.let { BCrypt.hashpw(it, BCrypt.gensalt()) }
        )
        println("UPDATED USER: $updated")
        return updated
    }

    @Transactional
    override fun convertGuest(userId: UUID, request: UserCreateRequest): UserCreateResponse {
        val current = getUser(userId)
        if (!current.isGuest) throw GuestConversionException("User is already registered")

        validateCredentials(request.username, request.email, request.password)
        ensureUnique(request.username, request.email, userId)

        val updated = userRepository.update(
            userId = userId,
            username = request.username.trim(),
            email = request.email.trim().lowercase(),
            passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt()),
            isGuest = false
        )

        val token = jwtService.generateToken(
            userId = updated.id,
            username = updated.username,
            isGuest = false
        )

        return UserCreateResponse(
            userId = updated.id,
            username = updated.username,
            email = updated.email,
            isGuest = false,
            token = token
        )
    }

    @Transactional
    override fun deleteUser(userId: UUID) {
        if (!userRepository.delete(userId)) throw UserNotFoundException()
    }

    private fun validateCredentials(username: String, email: String, password: String) {
        require(username.trim().isNotEmpty()) { "Username is required" }
        require(isValidEmail(email)) { "Valid email is required" }
        require(password.length >= 6) { "Password must be at least 6 characters" }
    }

    private fun ensureUnique(username: String, email: String, userId: UUID?) {
        if (userRepository.existsByUsername(username.trim(), userId)) throw UsernameAlreadyExistsException()
        if (userRepository.existsByEmail(email.trim(), userId)) throw EmailAlreadyExistsException()
    }

    private fun isValidEmail(email: String): Boolean =
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())
}