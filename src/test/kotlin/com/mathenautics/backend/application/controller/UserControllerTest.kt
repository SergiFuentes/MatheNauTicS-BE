package com.mathenautics.backend.application.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mathenautics.backend.application.exception.UserNotFoundException
import com.mathenautics.backend.application.service.UserService
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserCreateResponse
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
import com.mathenautics.backend.dto.UserUpdateResponse
import com.mathenautics.backend.security.AuthenticatedUser
import com.mathenautics.backend.security.JwtService
import com.mathenautics.backend.security.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.util.*

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var jwtService: JwtService

    @MockkBean
    private lateinit var userRepository: UserRepository

    private val testUserId = UUID.randomUUID()
    private val testUsername = "testuser"
    private val testEmail = "test@example.com"
    private val testCreatedAt = OffsetDateTime.now()

    private val authenticatedUser = AuthenticatedUser(
        userId = testUserId,
        username = testUsername,
        isGuest = false
    )

    private val authentication = mockk<Authentication> {
        every { principal } returns authenticatedUser
        every { isAuthenticated } returns true
        every { name } returns testUsername
    }

    @Test
    fun `createUser should return UserCreateResponse`() {
        val request = UserCreateRequest("newuser", "new@example.com", "password", false)
        val response = UserCreateResponse(
            userId = UUID.randomUUID(),
            username = "newuser",
            email = "new@example.com",
            isGuest = false,
            token = "jwt.token"
        )

        every { userService.createUser(any()) } returns response

        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.token").value("jwt.token"))
    }

    @Test
    fun `getCurrentUser should return UserResponse when authenticated`() {
        val userResponse = UserResponse(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            createdAt = testCreatedAt,
            isGuest = false
        )

        every { userService.getUser(testUserId) } returns userResponse

        mockMvc.perform(
            get("/api/v1/users/me").with(authentication(authentication))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testUserId.toString()))
            .andExpect(jsonPath("$.username").value(testUsername))
    }

    @Test
    fun `getCurrentUser should return 404 when user not found`() {
        every { userService.getUser(testUserId) } throws UserNotFoundException()

        mockMvc.perform(
            get("/api/v1/users/me").with(authentication(authentication))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `updateCurrentUser should return updated UserUpdateResponse with refreshed token`() {
        val request = UserUpdateRequest(username = "updated")
        val updated = UserUpdateResponse(
            id = testUserId,
            username = "updated",
            email = testEmail,
            createdAt = testCreatedAt,
            isGuest = false,
            token = "jwt.refreshed"
        )

        every { userService.updateUser(testUserId, any()) } returns updated

        mockMvc.perform(
            put("/api/v1/users/me")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("updated"))
            .andExpect(jsonPath("$.token").value("jwt.refreshed"))

        verify(exactly = 1) {
            userService.updateUser(
                testUserId,
                match {
                    it.username == "updated" &&
                            it.email == null &&
                            it.password == null &&
                            it.currentPassword == null
                }
            )
        }
    }

    @Test
    fun `updateCurrentUser should forward currentPassword to the service`() {
        val request = UserUpdateRequest(password = "newpass1", currentPassword = "oldpass1")
        val updated = UserUpdateResponse(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            createdAt = testCreatedAt,
            isGuest = false,
            token = "jwt.rotated"
        )

        every { userService.updateUser(testUserId, any()) } returns updated

        mockMvc.perform(
            put("/api/v1/users/me")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("jwt.rotated"))

        verify(exactly = 1) {
            userService.updateUser(
                testUserId,
                match {
                    it.password == "newpass1" && it.currentPassword == "oldpass1"
                }
            )
        }
    }

    @Test
    fun `convertGuest should return UserCreateResponse`() {
        val request = UserCreateRequest("registered", "reg@test.com", "pass", false)
        val response = UserCreateResponse(
            userId = testUserId,
            username = "registered",
            email = "reg@test.com",
            isGuest = false,
            token = "jwt.token"
        )

        every { userService.convertGuest(testUserId, any()) } returns response

        mockMvc.perform(
            post("/api/v1/users/me/convert")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("registered"))
            .andExpect(jsonPath("$.isGuest").value(false))
    }

    @Test
    fun `deleteCurrentUser should return 204 No Content`() {
        every { userService.deleteUser(testUserId) } returns Unit

        mockMvc.perform(
            delete("/api/v1/users/me").with(authentication(authentication))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `unauthenticated request to me endpoint should return 401`() {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized)
    }
}