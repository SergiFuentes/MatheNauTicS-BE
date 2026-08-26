package com.mathenautics.backend.application.service.impl

import com.mathenautics.backend.application.service.UserService
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    @Transactional
    override fun createUser(request: UserCreateRequest): UserResponse {
        require(request.username.isNotBlank()) { "Username is required" }
        require(request.email.isNotBlank()) { "Email is required" }
        require(request.password.length >= 6) { "Password must be at least 6 characters" }

        val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())

        return userRepository.create(
            username = request.username,
            email = request.email,
            passwordHash = passwordHash,
            isGuest = request.isGuest
        )
    }

    override fun getUser(userId: UUID): UserResponse {
        return userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found with id: $userId")
    }

    @Transactional
    override fun updateUser(userId: UUID, request: UserUpdateRequest): UserResponse {
        val passwordHash = request.password?.let { BCrypt.hashpw(it, BCrypt.gensalt()) }
        return userRepository.update(userId, request.username, request.email, passwordHash)
    }

    @Transactional
    override fun deleteUser(userId: UUID) {
        val deleted = userRepository.delete(userId)
        if (!deleted) {
            throw IllegalArgumentException("User not found with id: $userId")
        }
    }
}