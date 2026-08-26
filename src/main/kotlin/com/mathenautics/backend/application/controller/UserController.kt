// UserController.kt
package com.mathenautics.backend.application.controller

import com.mathenautics.backend.application.service.UserService
import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    @PostMapping
    fun createUser(@RequestBody request: UserCreateRequest): ResponseEntity<UserResponse> {
        val created = userService.createUser(request)
        return ResponseEntity.ok(created)
    }

    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: UUID): ResponseEntity<UserResponse> {
        val user = userService.getUser(userId)
        return ResponseEntity.ok(user)
    }

    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: UUID,
        @RequestBody request: UserUpdateRequest
    ): ResponseEntity<UserResponse> {
        val updated = userService.updateUser(userId, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: UUID): ResponseEntity<Unit> {
        userService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }
}