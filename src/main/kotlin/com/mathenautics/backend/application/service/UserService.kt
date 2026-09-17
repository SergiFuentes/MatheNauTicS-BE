package com.mathenautics.backend.application.service

import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserCreateResponse
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
import com.mathenautics.backend.dto.UserUpdateResponse
import java.util.UUID

interface UserService {
    fun createUser(request: UserCreateRequest): UserCreateResponse
    fun getUser(userId: UUID): UserResponse
    fun updateUser(userId: UUID, request: UserUpdateRequest): UserUpdateResponse
    fun convertGuest(userId: UUID, request: UserCreateRequest): UserCreateResponse
    fun deleteUser(userId: UUID)
}