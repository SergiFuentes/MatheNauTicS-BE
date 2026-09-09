package com.mathenautics.backend.integration.repository

import com.mathenautics.backend.domain.model.UserCredentials
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.infrastructure.repository.JdbcUserRepository
import com.mathenautics.backend.integration.IntegrationTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mindrot.jbcrypt.BCrypt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcUserRepositoryIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var userRepository: JdbcUserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val testUsername = "testuser"
    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val testPasswordHash = BCrypt.hashpw(testPassword, BCrypt.gensalt())

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM users")
    }

    @Test
    fun `create should insert a new user and return UserResponse`() {
        // Given
        val username = "newuser"
        val email = "new@example.com"
        val passwordHash = BCrypt.hashpw("pass", BCrypt.gensalt())

        // When
        val created = userRepository.create(username, email, passwordHash, isGuest = false)

        // Then
        assertNotNull(created.id)
        assertEquals(username, created.username)
        assertEquals(email, created.email)
        assertFalse(created.isGuest)
        assertNotNull(created.createdAt)

        // Verify in database
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Int::class.java, username) ?: 0
        assertEquals(1, count)
    }

    @Test
    fun `findById should return UserResponse for existing user`() {
        // Given
        val userId = insertTestUser()

        // When
        val found = userRepository.findById(userId)

        // Then
        assertNotNull(found)
        assertEquals(testUsername, found?.username)
        assertEquals(testEmail, found?.email)
        assertFalse(found?.isGuest ?: true)
    }

    @Test
    fun `findById should return null for non-existing user`() {
        // When
        val found = userRepository.findById(UUID.randomUUID())

        // Then
        assertNull(found)
    }

    @Test
    fun `findCredentialsByIdentifier should return credentials for username`() {
        // Given
        insertTestUser()

        // When
        val credentials = userRepository.findCredentialsByIdentifier(testUsername)

        // Then
        assertNotNull(credentials)
        assertEquals(testUsername, credentials?.username)
        assertEquals(testEmail, credentials?.email)
        assertEquals(testPasswordHash, credentials?.passwordHash)
        assertFalse(credentials?.isGuest ?: true)
    }

    @Test
    fun `findCredentialsByIdentifier should return credentials for email`() {
        // Given
        insertTestUser()

        // When
        val credentials = userRepository.findCredentialsByIdentifier(testEmail)

        // Then
        assertNotNull(credentials)
        assertEquals(testUsername, credentials?.username)
        assertEquals(testEmail, credentials?.email)
    }

    @Test
    fun `findCredentialsByIdentifier should return null for unknown identifier`() {
        // When
        val credentials = userRepository.findCredentialsByIdentifier("unknown")

        // Then
        assertNull(credentials)
    }

    @Test
    fun `existsByUsername should return true for existing username`() {
        // Given
        insertTestUser()

        // When
        val exists = userRepository.existsByUsername(testUsername, null)

        // Then
        assertTrue(exists)
    }

    @Test
    fun `existsByUsername should return false for non-existing username`() {
        // When
        val exists = userRepository.existsByUsername("nonexistent", null)

        // Then
        assertFalse(exists)
    }

    @Test
    fun `existsByUsername should exclude provided userId when checking`() {
        // Given
        val userId = insertTestUser()

        // When
        val exists = userRepository.existsByUsername(testUsername, userId)

        // Then
        // The user exists, but we're excluding itself, so should return false
        assertFalse(exists)
    }

    @Test
    fun `existsByEmail should return true for existing email`() {
        // Given
        insertTestUser()

        // When
        val exists = userRepository.existsByEmail(testEmail, null)

        // Then
        assertTrue(exists)
    }

    @Test
    fun `existsByEmail should return false for non-existing email`() {
        // When
        val exists = userRepository.existsByEmail("nonexistent@example.com", null)

        // Then
        assertFalse(exists)
    }

    @Test
    fun `update should modify user fields and return updated UserResponse`() {
        // Given
        val userId = insertTestUser()
        val newUsername = "newusername"
        val newEmail = "new@example.com"
        val newPassword = "newpassword123"
        val newPasswordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())

        // When
        val updated = userRepository.update(userId, newUsername, newEmail, newPasswordHash, null)

        // Then
        assertEquals(userId, updated.id)
        assertEquals(newUsername, updated.username)
        assertEquals(newEmail, updated.email)
        // isGuest should remain as it was (false)
        assertFalse(updated.isGuest)

        // Verify password in database using BCrypt
        val dbHash = jdbcTemplate.queryForObject(
            "SELECT password_hash FROM users WHERE id = ?",
            String::class.java,
            userId
        )
        assertTrue(BCrypt.checkpw(newPassword, dbHash), "Password should be updated")
    }

    @Test
    fun `update with null fields should keep existing values`() {
        // Given
        val userId = insertTestUser()

        // When
        val updated = userRepository.update(userId, null, null, null, null)

        // Then
        assertEquals(testUsername, updated.username)
        assertEquals(testEmail, updated.email)
        // Password should remain the same
        val dbHash = jdbcTemplate.queryForObject(
            "SELECT password_hash FROM users WHERE id = ?",
            String::class.java,
            userId
        )
        assertEquals(testPasswordHash, dbHash)
    }

    @Test
    fun `delete should remove the user and return true`() {
        // Given
        val userId = insertTestUser()

        // When
        val deleted = userRepository.delete(userId)

        // Then
        assertTrue(deleted)
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Int::class.java, userId) ?: 0
        assertEquals(0, count)
    }

    @Test
    fun `delete should return false for non-existing user`() {
        // When
        val deleted = userRepository.delete(UUID.randomUUID())

        // Then
        assertFalse(deleted)
    }

    // Helper method to insert a test user and return its UUID
    private fun insertTestUser(): UUID {
        val userId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, email, password_hash, is_guest, created_at)
            VALUES (?, ?, ?, ?, ?, NOW())
            """,
            userId, testUsername, testEmail, testPasswordHash, false
        )
        return userId
    }
}