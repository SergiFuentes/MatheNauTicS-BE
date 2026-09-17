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
    private val testTokenVersion = 4

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        jwtService = mockk()
        authService = AuthServiceImpl(userRepository, jwtService)
    }

    @Test
    fun `login with valid username should return LoginResponse with token and current token version`() {
        val request = LoginRequest(testUsername, testPassword)
        val credentials = UserCredentials(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            passwordHash = testPasswordHash,
            isGuest = testIsGuest,
            tokenVersion = testTokenVersion
        )

        every { userRepository.findCredentialsByIdentifier(testUsername) } returns credentials
        every {
            jwtService.generateToken(testUserId, testUsername, testIsGuest, testTokenVersion)
        } returns "jwt.token"

        val response = authService.login(request)

        assertEquals(testUserId, response.userId)
        assertEquals(testUsername, response.username)
        assertEquals(testEmail, response.email)
        assertFalse(response.isGuest)
        assertEquals("jwt.token", response.token)

        verify(exactly = 1) {
            jwtService.generateToken(testUserId, testUsername, testIsGuest, testTokenVersion)
        }
    }

    @Test
    fun `login with valid email should return LoginResponse with token`() {
        val request = LoginRequest(testEmail, testPassword)
        val credentials = UserCredentials(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            passwordHash = testPasswordHash,
            isGuest = testIsGuest,
            tokenVersion = 0
        )

        every { userRepository.findCredentialsByIdentifier(testEmail) } returns credentials
        every { jwtService.generateToken(testUserId, testUsername, testIsGuest, 0) } returns "jwt.token"

        val response = authService.login(request)
        assertEquals(testUserId, response.userId)
    }

    @Test
    fun `login with guest user should return isGuest true`() {
        val identifier = "guest_user"
        val password = "guestPass"
        val request = LoginRequest(identifier, password)
        val guestCredentials = UserCredentials(
            id = testUserId,
            username = "guest_user",
            email = "guest@example.com",
            passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
            isGuest = true,
            tokenVersion = 0
        )

        every { userRepository.findCredentialsByIdentifier(identifier) } returns guestCredentials
        every { jwtService.generateToken(testUserId, "guest_user", true, 0) } returns "jwt.token"

        val response = authService.login(request)
        assertTrue(response.isGuest)
        verify { jwtService.generateToken(testUserId, "guest_user", true, 0) }
    }

    @Test
    fun `login with empty identifier should throw IllegalArgumentException`() {
        val request = LoginRequest("   ", "password")
        val ex = assertThrows(IllegalArgumentException::class.java) { authService.login(request) }
        assertEquals("Username or email is required", ex.message)
        verify(exactly = 0) { userRepository.findCredentialsByIdentifier(any()) }
    }

    @Test
    fun `login with empty password should throw IllegalArgumentException`() {
        val request = LoginRequest("valid_user", "")
        val ex = assertThrows(IllegalArgumentException::class.java) { authService.login(request) }
        assertEquals("Password is required", ex.message)
    }

    @Test
    fun `login with non-existent identifier should throw InvalidCredentialsException`() {
        every { userRepository.findCredentialsByIdentifier("unknown") } returns null
        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(LoginRequest("unknown", "password"))
        }
    }

    @Test
    fun `login with incorrect password should throw InvalidCredentialsException`() {
        val credentials = UserCredentials(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            passwordHash = testPasswordHash,
            isGuest = testIsGuest,
            tokenVersion = 0
        )
        every { userRepository.findCredentialsByIdentifier(testUsername) } returns credentials

        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(LoginRequest(testUsername, "wrongPass"))
        }
    }

    @Test
    fun `login with leading trailing spaces in identifier should trim and work`() {
        val credentials = UserCredentials(
            id = testUserId,
            username = testUsername,
            email = testEmail,
            passwordHash = testPasswordHash,
            isGuest = testIsGuest,
            tokenVersion = 1
        )
        every { userRepository.findCredentialsByIdentifier(testUsername) } returns credentials
        every { jwtService.generateToken(testUserId, testUsername, testIsGuest, 1) } returns "jwt.token"

        val response = authService.login(LoginRequest("  $testUsername  ", testPassword))
        assertEquals(testUserId, response.userId)
    }
}