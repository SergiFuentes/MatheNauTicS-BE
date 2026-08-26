package com.mathenautics.backend.application.service

import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
import java.util.UUID

/**
 * Service interface for user management.
 */
interface UserService {

    /**
     * Creates a new user.
     */
    fun createUser(request: UserCreateRequest): UserResponse

    /**
     * Retrieves a user by ID.
     */
    fun getUser(userId: UUID): UserResponse

    /**
     * Updates an existing user.
     */
    fun updateUser(userId: UUID, request: UserUpdateRequest): UserResponse

    /**
     * Deletes a user by ID.
     */
    fun deleteUser(userId: UUID)
}