package com.mathenautics.backend.infrastructure.repository

import java.sql.ResultSet
import java.time.OffsetDateTime

/**
 * Extension function for ResultSet to safely get an OffsetDateTime column.
 * Returns null if the column value is null.
 *
 * @param columnName the name of the column
 * @return OffsetDateTime value, or null if the column is null
 */
fun ResultSet.getOffsetDateTimeOrNull(columnName: String): OffsetDateTime? {
    val timestamp = getTimestamp(columnName)
    return timestamp?.toInstant()?.atOffset(OffsetDateTime.now().offset)
}

/**
 * Extension function for ResultSet to safely get an OffsetDateTime column.
 * Throws an exception if the column value is null.
 *
 * @param columnName the name of the column
 * @return OffsetDateTime value
 * @throws IllegalArgumentException if the column is null
 */
fun ResultSet.getOffsetDateTime(columnName: String): OffsetDateTime {
    return getOffsetDateTimeOrNull(columnName)
        ?: throw IllegalArgumentException("Column '$columnName' is null and cannot be converted to OffsetDateTime")
}