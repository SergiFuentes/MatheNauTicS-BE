package com.mathenautics.backend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expirationMs: Long
) {
    private val key: SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun generateToken(userId: UUID, username: String, isGuest: Boolean): String =
        Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("isGuest", isGuest.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun extractUserId(token: String): UUID {
        val subject = extractClaims(token).subject
        return UUID.fromString(subject)
    }

    fun extractUsername(token: String): String =
        extractClaims(token).get("username", String::class.java) ?: ""

    fun extractIsGuest(token: String): Boolean =
        (extractClaims(token).get("isGuest", String::class.java) ?: "false").toBoolean()

    fun isTokenValid(token: String): Boolean =
        try {
            extractClaims(token)
            true
        } catch (e: Exception) {
            false
        }

    private fun extractClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}