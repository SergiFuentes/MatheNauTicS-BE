package com.mathenautics.backend.util

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Extension function to safely convert any database object to OffsetDateTime.
 * Handles String, Timestamp, and OffsetDateTime inputs.
 */
fun Any?.toOffsetDateTime(): OffsetDateTime {
    return when (this) {
        is OffsetDateTime -> this
        is Timestamp -> this.toInstant().atOffset(ZoneOffset.UTC)
        is String -> {
            // Try to parse various formats
            try {
                OffsetDateTime.parse(this)
            } catch (e: Exception) {
                // If parsing fails, try to parse as Timestamp string
                try {
                    Timestamp.valueOf(this).toInstant().atOffset(ZoneOffset.UTC)
                } catch (e2: Exception) {
                    throw IllegalStateException("Cannot convert value to OffsetDateTime: $this", e2)
                }
            }
        }
        null -> throw IllegalStateException("Value is null")
        else -> throw IllegalStateException("Value is not an OffsetDateTime or convertible type: $this (${this::class.java})")
    }
}