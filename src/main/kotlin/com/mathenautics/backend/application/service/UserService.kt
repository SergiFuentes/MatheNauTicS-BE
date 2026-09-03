package com.mathenautics.backend.application.service

import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
import java.util.UUID

interface UserService {
    fun createUser(request: UserCreateRequest): UserResponse
    fun getUser(userId: UUID): UserResponse
    fun updateUser(userId: UUID, request: UserUpdateRequest): UserResponse
    fun convertGuest(userId: UUID, request: UserCreateRequest): UserResponse
    fun deleteUser(userId: UUID)
}
