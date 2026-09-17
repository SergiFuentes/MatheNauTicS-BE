package com.mathenautics.backend.application.service.impl

import com.mathenautics.backend.application.exception.EmailAlreadyExistsException
import com.mathenautics.backend.application.exception.GuestConversionException
import com.mathenautics.backend.application.exception.InvalidCredentialsException
import com.mathenautics.backend.application.exception.UsernameAlreadyExistsException
import com.mathenautics.backend.application.exception.UserNotFoundException
import com.mathenautics.backend.application.service.UserService
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserCreateResponse
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
import com.mathenautics.backend.dto.UserUpdateResponse
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

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private const val MAX_PASSWORD_BYTES = 72
        private val EMAIL_REGEX =
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

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
            isGuest = user.isGuest,
            tokenVersion = 0
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
    override fun updateUser(userId: UUID, request: UserUpdateRequest): UserUpdateResponse {
        val credentials = userRepository.findCredentialsById(userId)
            ?: throw UserNotFoundException()

        if (request.password != null) {
            val currentPassword = request.currentPassword
                ?: throw IllegalArgumentException("Current password is required to change the password")
            if (!BCrypt.checkpw(currentPassword, credentials.passwordHash)) {
                throw InvalidCredentialsException()
            }
            validatePassword(request.password)
        }

        request.username?.let {
            require(it.isNotBlank()) { "Username is required" }
            if (userRepository.existsByUsername(it.trim(), userId)) throw UsernameAlreadyExistsException()
        }
        request.email?.let {
            require(isValidEmail(it)) { "Valid email is required" }
            if (userRepository.existsByEmail(it.trim(), userId)) throw EmailAlreadyExistsException()
        }

        val passwordChanged = request.password != null
        val newTokenVersion =
            if (passwordChanged) credentials.tokenVersion + 1 else credentials.tokenVersion

        val updated = userRepository.update(
            userId = userId,
            username = request.username?.trim(),
            email = request.email?.trim()?.lowercase(),
            passwordHash = request.password?.let { BCrypt.hashpw(it, BCrypt.gensalt()) },
            isGuest = null,
            tokenVersion = if (passwordChanged) newTokenVersion else null
        )

        val newToken = jwtService.generateToken(
            userId = updated.id,
            username = updated.username,
            isGuest = updated.isGuest,
            tokenVersion = newTokenVersion
        )

        return UserUpdateResponse(
            id = updated.id,
            username = updated.username,
            email = updated.email,
            createdAt = updated.createdAt,
            isGuest = updated.isGuest,
            token = newToken
        )
    }

    @Transactional
    override fun convertGuest(userId: UUID, request: UserCreateRequest): UserCreateResponse {
        val current = getUser(userId)
        if (!current.isGuest) throw GuestConversionException("User is already registered")

        validateCredentials(request.username, request.email, request.password)
        ensureUnique(request.username, request.email, userId)

        val currentVersion = userRepository.findTokenVersionById(userId) ?: 0
        val newTokenVersion = currentVersion + 1

        val updated = userRepository.update(
            userId = userId,
            username = request.username.trim(),
            email = request.email.trim().lowercase(),
            passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt()),
            isGuest = false,
            tokenVersion = newTokenVersion
        )

        val token = jwtService.generateToken(
            userId = updated.id,
            username = updated.username,
            isGuest = false,
            tokenVersion = newTokenVersion
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
        validatePassword(password)
    }

    private fun validatePassword(password: String) {
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Password must be at least $MIN_PASSWORD_LENGTH characters"
        }
        require(password.toByteArray(Charsets.UTF_8).size <= MAX_PASSWORD_BYTES) {
            "Password exceeds the maximum length of $MAX_PASSWORD_BYTES bytes"
        }
    }

    private fun ensureUnique(username: String, email: String, userId: UUID?) {
        if (userRepository.existsByUsername(username.trim(), userId)) throw UsernameAlreadyExistsException()
        if (userRepository.existsByEmail(email.trim(), userId)) throw EmailAlreadyExistsException()
    }

    private fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
}