package com.mathenautics.backend.application.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mathenautics.backend.application.exception.InvalidCredentialsException
import com.mathenautics.backend.application.service.AuthService
import com.mathenautics.backend.dto.LoginRequest
import com.mathenautics.backend.dto.LoginResponse
import com.mathenautics.backend.security.JwtAuthenticationFilter
import com.mathenautics.backend.security.JwtService
import com.mathenautics.backend.security.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class, JwtAuthenticationFilter::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var authService: AuthService

    @MockkBean
    private lateinit var jwtService: JwtService

    @Test
    fun `login with valid credentials should return LoginResponse`() {
        // Given
        val request = LoginRequest("testuser", "password")
        val response = LoginResponse(
            userId = UUID.randomUUID(),
            username = "testuser",
            email = "test@example.com",
            isGuest = false,
            token = "jwt.token"
        )

        every { authService.login(any()) } returns response

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(response.userId.toString()))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.isGuest").value(false))
            .andExpect(jsonPath("$.token").value("jwt.token"))

        // Verify the service was called with the correct request
        val capturedRequest = slot<LoginRequest>()

        verify(exactly = 1) {
            authService.login(capture(capturedRequest))
        }

        assertEquals("testuser", capturedRequest.captured.identifier)
        assertEquals("password", capturedRequest.captured.password)
    }

    @Test
    fun `login with invalid credentials should return 401 Unauthorized`() {
        // Given
        val request = LoginRequest("wrong", "wrong")

        every { authService.login(any()) } throws InvalidCredentialsException()

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Invalid username/email or password"))
    }
}
