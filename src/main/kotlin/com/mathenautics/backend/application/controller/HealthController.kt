package com.mathenautics.backend.application.controller

import org.springframework.boot.context.properties.bind.Bindable.mapOf
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController(
    private val jdbcTemplate: JdbcTemplate
) {

    @GetMapping("/health")
    fun health(): Map<String, Any> {  // Cambiado de String a Any porque los valores pueden ser String o null
        return try {
            jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            mapOf(
                "status" to "UP",
                "database" to "connected"
            )
        } catch (e: Exception) {
            mapOf(
                "status" to "DOWN",
                "database" to "disconnected",
                "error" to (e.message ?: "Unknown error")
            )
        }
    }

    @GetMapping("/")
    fun home(): String {
        return "MatheNauTicS Backend - Running!"
    }
}