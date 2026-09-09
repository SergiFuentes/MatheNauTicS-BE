package com.mathenautics.backend.application.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mathenautics.backend.application.exception.UserNotFoundException
import com.mathenautics.backend.application.service.UserService
import com.mathenautics.backend.dto.UserCreateRequest
import com.mathenautics.backend.dto.UserCreateResponse
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.dto.UserUpdateRequest
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
        // Given
        val request = UserCreateRequest(
            "newuser",
            "new@example.com",
            "password",
            false
        )

        val response = UserCreateResponse(
            userId = UUID.randomUUID(),
            username = "newuser",
            email = "new@example.com",
            isGuest = false,
            token = "jwt.token"
        )

        every {
            userService.createUser(any())
        } returns response

        // When & Then
        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(response.userId.toString()))
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.email").value("new@example.com"))
            .andExpect(jsonPath("$.isGuest").value(false))
            .andExpect(jsonPath("$.token").value("jwt.token"))

        verify(exactly = 1) {
            userService.createUser(
                match {
                    it.username == "newuser" &&
                            it.email == "new@example.com" &&
                            it.password == "password" &&
                            !it.isGuest
                }
            )
        }
    }

    @Test
    fun `getCurrentUser should return UserResponse when authenticated`() {
        // Given
        val userResponse = UserResponse(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            createdAt = testCreatedAt,
            isGuest = false
        )

        every {
            userService.getUser(testUserId)
        } returns userResponse

        // When & Then
        mockMvc.perform(
            get("/api/v1/users/me")
                .with(authentication(authentication))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testUserId.toString()))
            .andExpect(jsonPath("$.username").value(testUsername))
            .andExpect(jsonPath("$.email").value(testEmail))
            .andExpect(jsonPath("$.isGuest").value(false))
            .andExpect(jsonPath("$.createdAt").exists())

        verify(exactly = 1) {
            userService.getUser(testUserId)
        }
    }

    @Test
    fun `getCurrentUser should return 404 when user not found`() {
        // Given
        every {
            userService.getUser(testUserId)
        } throws UserNotFoundException()

        // When & Then
        mockMvc.perform(
            get("/api/v1/users/me")
                .with(authentication(authentication))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("User not found"))
    }

    @Test
    fun `updateCurrentUser should return updated UserResponse`() {
        // Given
        val request = UserUpdateRequest(username = "updated")

        val updated = UserResponse(
            id = testUserId,
            username = "updated",
            email = testEmail,
            createdAt = testCreatedAt,
            isGuest = false
        )

        every {
            userService.updateUser(
                testUserId,
                any()
            )
        } returns updated

        // When & Then
        mockMvc.perform(
            put("/api/v1/users/me")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("updated"))
            .andExpect(jsonPath("$.email").value(testEmail))

        verify(exactly = 1) {
            userService.updateUser(
                testUserId,
                match {
                    it.username == "updated" &&
                            it.email == null &&
                            it.password == null
                }
            )
        }
    }

    @Test
    fun `convertGuest should return UserCreateResponse`() {
        // Given
        val request = UserCreateRequest(
            "registered",
            "reg@test.com",
            "pass",
            false
        )

        val response = UserCreateResponse(
            userId = testUserId,
            username = "registered",
            email = "reg@test.com",
            isGuest = false,
            token = "jwt.token"
        )

        every {
            userService.convertGuest(
                testUserId,
                any()
            )
        } returns response

        // When & Then
        mockMvc.perform(
            post("/api/v1/users/me/convert")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(testUserId.toString()))
            .andExpect(jsonPath("$.username").value("registered"))
            .andExpect(jsonPath("$.email").value("reg@test.com"))
            .andExpect(jsonPath("$.isGuest").value(false))
            .andExpect(jsonPath("$.token").value("jwt.token"))

        verify(exactly = 1) {
            userService.convertGuest(
                testUserId,
                match {
                    it.username == "registered" &&
                            it.email == "reg@test.com" &&
                            it.password == "pass" &&
                            !it.isGuest
                }
            )
        }
    }

    @Test
    fun `deleteCurrentUser should return 204 No Content`() {
        // Given
        every {
            userService.deleteUser(testUserId)
        } returns Unit

        // When & Then
        mockMvc.perform(
            delete("/api/v1/users/me")
                .with(authentication(authentication))
        )
            .andExpect(status().isNoContent)

        verify(exactly = 1) {
            userService.deleteUser(testUserId)
        }
    }

    @Test
    fun `unauthenticated request to me endpoint should return 401`() {
        mockMvc.perform(
            get("/api/v1/users/me")
        )
            .andExpect(status().isUnauthorized)
    }
}
