package com.mathenautics.backend.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security.rate-limit")
data class RateLimitProperties(
    val login: Rule,
    val registration: Rule
) {
    data class Rule(
        val maxRequests: Int,
        val windowSeconds: Int
    )
}