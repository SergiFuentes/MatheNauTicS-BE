package com.mathenautics.backend.infrastructure.repository

import com.mathenautics.backend.domain.model.UserCredentials
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.UserResponse
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class JdbcUserRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : UserRepository {
    override fun existsById(userId: UUID): Boolean =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE id = :userId",
            MapSqlParameterSource("userId", userId),
            Int::class.java
        ) ?: 0 > 0

    override fun existsByUsername(username: String, excludingUserId: UUID?): Boolean {
        val sql = if (excludingUserId == null) {
            "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(:username)"
        } else {
            "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(:username) AND id <> :userId"
        }
        val params = MapSqlParameterSource("username", username).apply {
            excludingUserId?.let { addValue("userId", it) }
        }
        return (jdbcTemplate.queryForObject(sql, params, Int::class.java) ?: 0) > 0
    }

    override fun existsByEmail(email: String, excludingUserId: UUID?): Boolean {
        val sql = if (excludingUserId == null) {
            "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email)"
        } else {
            "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email) AND id <> :userId"
        }
        val params = MapSqlParameterSource("email", email).apply {
            excludingUserId?.let { addValue("userId", it) }
        }
        return (jdbcTemplate.queryForObject(sql, params, Int::class.java) ?: 0) > 0
    }

    override fun create(username: String, email: String, passwordHash: String, isGuest: Boolean): UserResponse {
        val sql = """
            INSERT INTO users (username, email, password_hash, is_guest)
            VALUES (:username, :email, :passwordHash, :isGuest)
            RETURNING id, username, email, created_at, is_guest
        """
        return jdbcTemplate.query(sql, params(username, email, passwordHash, isGuest)) { rs, _ ->
            mapUser(rs)
        }.single()
    }

    override fun findById(userId: UUID): UserResponse? = jdbcTemplate.query(
        "SELECT id, username, email, created_at, is_guest FROM users WHERE id = :userId",
        MapSqlParameterSource("userId", userId)
    ) { rs, _ -> mapUser(rs) }.singleOrNull()

    override fun findCredentialsByIdentifier(identifier: String): UserCredentials? {
        val sql = """
            SELECT id, username, email, password_hash, is_guest
            FROM users
            WHERE LOWER(username) = LOWER(:identifier)
               OR LOWER(email) = LOWER(:identifier)
            LIMIT 1
        """
        return jdbcTemplate.query(sql, MapSqlParameterSource("identifier", identifier)) { rs, _ ->
            UserCredentials(
                id = rs.getObject("id", UUID::class.java),
                username = rs.getString("username"),
                email = rs.getString("email"),
                passwordHash = rs.getString("password_hash"),
                isGuest = rs.getBoolean("is_guest")
            )
        }.singleOrNull()
    }

    override fun update(
        userId: UUID,
        username: String?,
        email: String?,
        passwordHash: String?,
        isGuest: Boolean?
    ): UserResponse {
        val updates = mutableListOf<String>()
        val params = MapSqlParameterSource("userId", userId)

        username?.let { updates += "username = :username"; params.addValue("username", it) }
        email?.let { updates += "email = :email"; params.addValue("email", it) }
        passwordHash?.let { updates += "password_hash = :passwordHash"; params.addValue("passwordHash", it) }
        isGuest?.let { updates += "is_guest = :isGuest"; params.addValue("isGuest", it) }

        if (updates.isEmpty()) {
            return findById(userId)
                ?: throw IllegalArgumentException("User not found")
        }

        val sql = """
        UPDATE users
        SET ${updates.joinToString(", ")}
        WHERE id = :userId
        RETURNING id, username, email, created_at, is_guest
    """

        return jdbcTemplate.query(sql, params) { rs, _ -> mapUser(rs) }
            .singleOrNull()
            ?: throw IllegalArgumentException("User not found")
    }

    override fun delete(userId: UUID): Boolean =
        jdbcTemplate.update(
            "DELETE FROM users WHERE id = :userId",
            MapSqlParameterSource("userId", userId)
        ) > 0

    private fun params(username: String, email: String, passwordHash: String, isGuest: Boolean) =
        MapSqlParameterSource()
            .addValue("username", username)
            .addValue("email", email)
            .addValue("passwordHash", passwordHash)
            .addValue("isGuest", isGuest)

    private fun mapUser(rs: java.sql.ResultSet) = UserResponse(
        id = rs.getObject("id", UUID::class.java),
        username = rs.getString("username"),
        email = rs.getString("email"),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
        isGuest = rs.getBoolean("is_guest")
    )
}
