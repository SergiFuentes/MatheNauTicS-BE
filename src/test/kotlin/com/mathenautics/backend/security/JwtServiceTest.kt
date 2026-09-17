package com.mathenautics.backend.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    private val secret = "test-secret-for-jwt-tests-only-1234567890abcdef"
    private val expirationMs = 3600000L

    @BeforeEach
    fun setUp() {
        jwtService = JwtService(secret, expirationMs)
    }

    @Test
    fun `generateToken should return a non-empty token`() {
        val token = jwtService.generateToken(UUID.randomUUID(), "testuser", false, 0)
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `extractUserId should return the correct UUID from token`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId, "testuser", true, 3)
        assertEquals(userId, jwtService.extractUserId(token))
    }

    @Test
    fun `extractUsername should return the correct username from token`() {
        val token = jwtService.generateToken(UUID.randomUUID(), "testuser", false, 0)
        assertEquals("testuser", jwtService.extractUsername(token))
    }

    @Test
    fun `extractIsGuest should return the correct isGuest flag from token`() {
        val token = jwtService.generateToken(UUID.randomUUID(), "testuser", true, 0)
        assertTrue(jwtService.extractIsGuest(token))
    }

    @Test
    fun `extractIsGuest should return false when claim is missing`() {
        val userId = UUID.randomUUID()
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        val token = Jwts.builder()
            .subject(userId.toString())
            .claim("username", "testuser")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

        assertFalse(jwtService.extractIsGuest(token))
    }

    @Test
    fun `extractTokenVersion should return the value embedded at issue time`() {
        val token = jwtService.generateToken(UUID.randomUUID(), "testuser", false, 7)
        assertEquals(7, jwtService.extractTokenVersion(token))
    }

    @Test
    fun `extractTokenVersion should return 0 when claim is missing`() {
        val userId = UUID.randomUUID()
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        val token = Jwts.builder()
            .subject(userId.toString())
            .claim("username", "testuser")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

        assertEquals(0, jwtService.extractTokenVersion(token))
    }

    @Test
    fun `isTokenValid should return true for a valid token`() {
        val token = jwtService.generateToken(UUID.randomUUID(), "testuser", false, 0)
        assertTrue(jwtService.isTokenValid(token))
    }

    @Test
    fun `isTokenValid should return false for an expired token`() {
        val shortExpirationService = JwtService(secret, 1)
        val token = shortExpirationService.generateToken(UUID.randomUUID(), "testuser", false, 0)
        Thread.sleep(10)
        assertFalse(shortExpirationService.isTokenValid(token))
    }

    @Test
    fun `isTokenValid should return false for a malformed token`() {
        assertFalse(jwtService.isTokenValid("this.is.not.a.jwt"))
    }

    @Test
    fun `isTokenValid should return false for a token with wrong signature`() {
        val wrongSecret = "wrongSecretKeyThatIsAtLeast32CharactersLongForHS256"
        val wrongJwtService = JwtService(wrongSecret, expirationMs)
        val token = wrongJwtService.generateToken(UUID.randomUUID(), "testuser", false, 0)
        assertFalse(jwtService.isTokenValid(token))
    }

    @Test
    fun `extractUserId should throw exception for invalid token`() {
        assertThrows(Exception::class.java) {
            jwtService.extractUserId("invalid.token.here")
        }
    }

    @Test
    fun `extractUsername should return empty string for missing claim`() {
        val userId = UUID.randomUUID()
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        val token = Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

        assertEquals("", jwtService.extractUsername(token))
    }
}