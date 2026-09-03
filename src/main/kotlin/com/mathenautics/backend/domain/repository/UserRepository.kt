package com.mathenautics.backend.domain.repository

import com.mathenautics.backend.domain.model.UserCredentials
import com.mathenautics.backend.dto.UserResponse
import java.util.UUID

interface UserRepository {
    fun existsById(userId: UUID): Boolean
    fun existsByUsername(username: String, excludingUserId: UUID? = null): Boolean
    fun existsByEmail(email: String, excludingUserId: UUID? = null): Boolean
    fun create(username: String, email: String, passwordHash: String, isGuest: Boolean): UserResponse
    fun findById(userId: UUID): UserResponse?
    fun findCredentialsByIdentifier(identifier: String): UserCredentials?
    fun update(userId: UUID, username: String?, email: String?, passwordHash: String?, isGuest: Boolean? = null): UserResponse
    fun delete(userId: UUID): Boolean
}
