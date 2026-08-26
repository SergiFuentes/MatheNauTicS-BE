package com.mathenautics.backend.domain.repository

import com.mathenautics.backend.dto.UserResponse
import java.util.UUID

interface UserRepository {
    fun existsById(userId: UUID): Boolean
    fun create(username: String, email: String, passwordHash: String, isGuest: Boolean): UserResponse
    fun findById(userId: UUID): UserResponse?
    fun update(userId: UUID, username: String?, email: String?, passwordHash: String?): UserResponse
    fun delete(userId: UUID): Boolean
}