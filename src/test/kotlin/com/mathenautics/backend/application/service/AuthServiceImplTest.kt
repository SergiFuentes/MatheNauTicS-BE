package com.mathenautics.backend.application.service.impl

import com.mathenautics.backend.application.exception.InvalidCredentialsException
import com.mathenautics.backend.domain.model.UserCredentials
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.LoginRequest
import com.mathenautics.backend.security.JwtService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

class AuthServiceImplTest {

    private lateinit var userRepository: UserRepository
    private lateinit var jwtService: JwtService
    private lateinit var authService: AuthServiceImpl

    private val testPassword = "securePass123"
    private val testPasswordHash = BCrypt.hashpw(testPassword, BCrypt.gensalt())
    private val testUserId = UUID.randomUUID()
    private val testUsername = "testuser"
    private val testEmail = "test@example.com"
    private val testIsGuest = false

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        jwtService = mockk()
        authService = AuthServiceImpl(userRepository, jwtService)
    }

    @Test
    fun `login with valid username should return LoginResponse with token`() {
        // Given
        val identifier = testUsername
        val password = testPassword
        val request = LoginRequest(identifier, password)

        val credentials = UserCredentials(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            passwordHash = testPasswordHash,
            isGuest = testIsGuest
        )

        every { userRepository.findCredentialsByIdentifier(identifier) } returns credentials
        every { jwtService.generateToken(testUserId, testUsername, testIsGuest) } returns "jwt.token"

        // When
        val response = authService.login(request)

        // Then
        assertEquals(testUserId, response.userId)
        assertEquals(testUsername, response.username)
        assertEquals(testEmail, response.email)
        assertEquals(testIsGuest, response.isGuest)
        assertEquals("jwt.token", response.token)

        verify(exactly = 1) { userRepository.findCredentialsByIdentifier(identifier) }
        verify(exactly = 1) { jwtService.generateToken(testUserId, testUsername, testIsGuest) }
    }

    @Test
    fun `login with valid email should return LoginResponse with token`() {
        // Given
        val identifier = testEmail
        val password = testPassword
        val request = LoginRequest(identifier, password)

        val credentials = UserCredentials(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            passwordHash = testPasswordHash,
            isGuest = testIsGuest
        )

        every { userRepository.findCredentialsByIdentifier(identifier) } returns credentials
        every { jwtService.generateToken(testUserId, testUsername, testIsGuest) } returns "jwt.token"

        // When
        val response = authService.login(request)

        // Then
        assertEquals(testUserId, response.userId)
        assertEquals(testUsername, response.username)
        assertEquals(testEmail, response.email)
        assertEquals(testIsGuest, response.isGuest)
        assertEquals("jwt.token", response.token)
    }

    @Test
    fun `login with guest user should return isGuest true`() {
        // Given
        val identifier = "guest_user"
        val password = "guestPass"
        val request = LoginRequest(identifier, password)
        val guestCredentials = UserCredentials(
            id = testUserId,
            username = "guest_user",
            email = "guest@example.com",
            passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
            isGuest = true
        )

        every { userRepository.findCredentialsByIdentifier(identifier) } returns guestCredentials
        every { jwtService.generateToken(testUserId, "guest_user", true) } returns "jwt.token"

        // When
        val response = authService.login(request)

        // Then
        assertTrue(response.isGuest)
        verify { jwtService.generateToken(testUserId, "guest_user", true) }
    }

    @Test
    fun `login with empty identifier should throw IllegalArgumentException`() {
        // Given
        val request = LoginRequest("   ", "password")

        // When / Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.login(request)
        }
        assertEquals("Username or email is required", exception.message)
        verify(exactly = 0) { userRepository.findCredentialsByIdentifier(any()) }
        verify(exactly = 0) { jwtService.generateToken(any(), any(), any()) }
    }

    @Test
    fun `login with empty password should throw IllegalArgumentException`() {
        // Given
        val request = LoginRequest("valid_user", "")

        // When / Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.login(request)
        }
        assertEquals("Password is required", exception.message)
        verify(exactly = 0) { userRepository.findCredentialsByIdentifier(any()) }
        verify(exactly = 0) { jwtService.generateToken(any(), any(), any()) }
    }

    @Test
    fun `login with non-existent identifier should throw InvalidCredentialsException`() {
        // Given
        val identifier = "unknown"
        val request = LoginRequest(identifier, "password")

        every { userRepository.findCredentialsByIdentifier(identifier) } returns null

        // When / Then
        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(request)
        }
        verify(exactly = 1) { userRepository.findCredentialsByIdentifier(identifier) }
        verify(exactly = 0) { jwtService.generateToken(any(), any(), any()) }
    }

    @Test
    fun `login with incorrect password should throw InvalidCredentialsException`() {
        // Given
        val identifier = testUsername
        val wrongPassword = "wrongPass"
        val request = LoginRequest(identifier, wrongPassword)

        val credentials = UserCredentials(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            passwordHash = testPasswordHash,
            isGuest = testIsGuest
        )

        every { userRepository.findCredentialsByIdentifier(identifier) } returns credentials

        // When / Then
        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(request)
        }
        verify(exactly = 1) { userRepository.findCredentialsByIdentifier(identifier) }
        verify(exactly = 0) { jwtService.generateToken(any(), any(), any()) }
    }

    @Test
    fun `login with leading trailing spaces in identifier should trim and work`() {
        // Given
        val identifierWithSpaces = "  $testUsername  "
        val request = LoginRequest(identifierWithSpaces, testPassword)

        val credentials = UserCredentials(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            passwordHash = testPasswordHash,
            isGuest = testIsGuest
        )

        every { userRepository.findCredentialsByIdentifier(testUsername) } returns credentials
        every { jwtService.generateToken(testUserId, testUsername, testIsGuest) } returns "jwt.token"

        // When
        val response = authService.login(request)

        // Then
        assertEquals(testUserId, response.userId)
        verify { userRepository.findCredentialsByIdentifier(testUsername) }
    }
}