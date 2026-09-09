package com.mathenautics.backend.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    // Valid 64-character secret for HS256
    private val secret = "test-secret-for-jwt-tests-only-1234567890abcdef"
    private val expirationMs = 3600000L // 1 hour

    @BeforeEach
    fun setUp() {
        jwtService = JwtService(secret, expirationMs)
    }

    @Test
    fun `generateToken should return a non-empty token`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val isGuest = false

        val token = jwtService.generateToken(userId, username, isGuest)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `extractUserId should return the correct UUID from token`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val isGuest = true

        val token = jwtService.generateToken(userId, username, isGuest)
        val extractedUserId = jwtService.extractUserId(token)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `extractUsername should return the correct username from token`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val isGuest = false

        val token = jwtService.generateToken(userId, username, isGuest)
        val extractedUsername = jwtService.extractUsername(token)

        assertEquals(username, extractedUsername)
    }

    @Test
    fun `extractIsGuest should return the correct isGuest flag from token`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val isGuest = true

        val token = jwtService.generateToken(userId, username, isGuest)
        val extractedIsGuest = jwtService.extractIsGuest(token)

        assertTrue(extractedIsGuest)
    }

    @Test
    fun `extractIsGuest should return false when claim is missing`() {
        // Build a token without the "isGuest" claim using the same secret
        val userId = UUID.randomUUID()
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        val token = Jwts.builder()
            .subject(userId.toString())
            .claim("username", "testuser")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

        val extractedIsGuest = jwtService.extractIsGuest(token)

        assertFalse(extractedIsGuest)
    }

    @Test
    fun `isTokenValid should return true for a valid token`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val isGuest = false

        val token = jwtService.generateToken(userId, username, isGuest)

        assertTrue(jwtService.isTokenValid(token))
    }

    @Test
    fun `isTokenValid should return false for an expired token`() {
        // Use a service with very short expiration to force expiry
        val shortExpirationService = JwtService(secret, 1) // 1 ms
        val userId = UUID.randomUUID()
        val username = "testuser"
        val isGuest = false

        val token = shortExpirationService.generateToken(userId, username, isGuest)

        // Wait a bit for it to expire
        Thread.sleep(10)

        assertFalse(shortExpirationService.isTokenValid(token))
    }

    @Test
    fun `isTokenValid should return false for a malformed token`() {
        val malformedToken = "this.is.not.a.jwt"

        assertFalse(jwtService.isTokenValid(malformedToken))
    }

    @Test
    fun `isTokenValid should return false for a token with wrong signature`() {
        // Generate a token with a different secret
        val wrongSecret = "wrongSecretKeyThatIsAtLeast32CharactersLongForHS256"
        val wrongJwtService = JwtService(wrongSecret, expirationMs)
        val userId = UUID.randomUUID()
        val username = "testuser"
        val isGuest = false

        val token = wrongJwtService.generateToken(userId, username, isGuest)

        // Validate with original service (correct secret) -> should fail
        assertFalse(jwtService.isTokenValid(token))
    }

    @Test
    fun `extractUserId should throw exception for invalid token`() {
        val invalidToken = "invalid.token.here"

        assertThrows(Exception::class.java) {
            jwtService.extractUserId(invalidToken)
        }
    }

    @Test
    fun `extractUsername should return empty string for missing claim`() {
        // Build a token without the "username" claim
        val userId = UUID.randomUUID()
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        val token = Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

        val username = jwtService.extractUsername(token)
        assertEquals("", username)
    }
}