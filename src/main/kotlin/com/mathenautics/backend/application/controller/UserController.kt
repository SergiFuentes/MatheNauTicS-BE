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
    fun createUser(@RequestBody request: UserCreateRequest): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.createUser(request))

    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: UUID): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.getUser(userId))

    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: UUID,
        @RequestBody request: UserUpdateRequest
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.updateUser(userId, request))

    @PostMapping("/{userId}/convert")
    fun convertGuest(
        @PathVariable userId: UUID,
        @RequestBody request: UserCreateRequest
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(
        userService.convertGuest(userId, request.copy(isGuest = false))
    )

    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: UUID): ResponseEntity<Unit> {
        userService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }
}
