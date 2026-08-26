package com.mathenautics.backend.infrastructure.repository

import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.UserResponse
import com.mathenautics.backend.util.toOffsetDateTime
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class JdbcUserRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : UserRepository {

    override fun existsById(userId: UUID): Boolean {
        val sql = "SELECT COUNT(*) FROM users WHERE id = :userId"
        val params = MapSqlParameterSource().addValue("userId", userId)
        val count = jdbcTemplate.queryForObject(sql, params) { rs, _ ->
            rs.getInt(1)
        } ?: 0
        return count > 0
    }

    override fun create(username: String, email: String, passwordHash: String, isGuest: Boolean): UserResponse {
        val sql = """
            INSERT INTO users (username, email, password_hash, is_guest)
            VALUES (:username, :email, :passwordHash, :isGuest)
            RETURNING id, username, email, created_at, is_guest
        """
        val params = MapSqlParameterSource()
            .addValue("username", username)
            .addValue("email", email)
            .addValue("passwordHash", passwordHash)
            .addValue("isGuest", isGuest)

        return jdbcTemplate.query(sql, params) { rs, _ ->
            UserResponse(
                id = rs.getObject("id", UUID::class.java),
                username = rs.getString("username"),
                email = rs.getString("email"),
                createdAt = rs.getObject("created_at")?.toOffsetDateTime()
                    ?: throw IllegalStateException("created_at cannot be null"),
                isGuest = rs.getBoolean("is_guest")
            )
        }.single()
    }

    override fun findById(userId: UUID): UserResponse? {
        val sql = "SELECT id, username, email, created_at, is_guest FROM users WHERE id = :userId"
        val params = MapSqlParameterSource().addValue("userId", userId)

        return try {
            jdbcTemplate.query(sql, params) { rs, _ ->
                UserResponse(
                    id = rs.getObject("id", UUID::class.java),
                    username = rs.getString("username"),
                    email = rs.getString("email"),
                    createdAt = rs.getObject("created_at")?.toOffsetDateTime()
                        ?: throw IllegalStateException("created_at cannot be null"),
                    isGuest = rs.getBoolean("is_guest")
                )
            }.single()
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override fun update(userId: UUID, username: String?, email: String?, passwordHash: String?): UserResponse {
        val updates = mutableListOf<String>()
        val params = MapSqlParameterSource().addValue("userId", userId)

        username?.let { updates.add("username = :username"); params.addValue("username", it) }
        email?.let { updates.add("email = :email"); params.addValue("email", it) }
        passwordHash?.let { updates.add("password_hash = :passwordHash"); params.addValue("passwordHash", it) }

        if (updates.isEmpty()) {
            throw IllegalArgumentException("At least one field to update is required")
        }

        val sql = """
            UPDATE users
            SET ${updates.joinToString()}
            WHERE id = :userId
            RETURNING id, username, email, created_at, is_guest
        """

        return jdbcTemplate.query(sql, params) { rs, _ ->
            UserResponse(
                id = rs.getObject("id", UUID::class.java),
                username = rs.getString("username"),
                email = rs.getString("email"),
                createdAt = rs.getObject("created_at")?.toOffsetDateTime()
                    ?: throw IllegalStateException("created_at cannot be null"),
                isGuest = rs.getBoolean("is_guest")
            )
        }.single()
    }

    override fun delete(userId: UUID): Boolean {
        val sql = "DELETE FROM users WHERE id = :userId"
        val params = MapSqlParameterSource().addValue("userId", userId)
        return jdbcTemplate.update(sql, params) > 0
    }
}