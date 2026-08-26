package com.mathenautics.backend.infrastructure.repository

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource

/**
 * Extension function for NamedParameterJdbcTemplate that returns a non-nullable result.
 * Throws an exception if the query returns null.
 */
fun <T : Any> NamedParameterJdbcTemplate.queryForObjectNonNull(
    sql: String,
    paramSource: SqlParameterSource,
    rowMapper: RowMapper<T>
): T {
    return this.queryForObject(sql, paramSource, rowMapper) ?: throw IllegalStateException("Query returned null")
}