package com.mathenautics.backend.application.controller

import com.mathenautics.backend.application.service.UserService
import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserCreateResponse
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
import com.mathenautics.backend.security.AuthenticatedUser
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    @PostMapping
    fun createUser(@RequestBody request: UserCreateRequest): ResponseEntity<UserCreateResponse> =
        ResponseEntity.ok(userService.createUser(request))

    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<UserResponse> {
        val authenticatedUser = authentication.principal as AuthenticatedUser
        val user = userService.getUser(authenticatedUser.userId)
        return ResponseEntity.ok(user)
    }

    @PutMapping("/me")
    fun updateCurrentUser(
        @RequestBody request: UserUpdateRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {
        val authenticatedUser = authentication.principal as AuthenticatedUser
        val updated = userService.updateUser(authenticatedUser.userId, request)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/me/convert")
    fun convertGuest(
        @RequestBody request: UserCreateRequest,
        authentication: Authentication
    ): ResponseEntity<UserCreateResponse> {
        val authenticatedUser = authentication.principal as AuthenticatedUser
        val converted = userService.convertGuest(authenticatedUser.userId, request)
        return ResponseEntity.ok(converted)
    }

    @DeleteMapping("/me")
    fun deleteCurrentUser(authentication: Authentication): ResponseEntity<Unit> {
        val authenticatedUser = authentication.principal as AuthenticatedUser
        userService.deleteUser(authenticatedUser.userId)
        return ResponseEntity.noContent().build()
    }
}