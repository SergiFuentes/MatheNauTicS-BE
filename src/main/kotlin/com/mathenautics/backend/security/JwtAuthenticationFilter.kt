package com.mathenautics.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    companion object {
        private const val AUTH_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.method == "OPTIONS") {
            filterChain.doFilter(request, response)
            return
        }
        val token = extractToken(request)
        logger.info("Token extraído: $token")

        if (token != null && jwtService.isTokenValid(token)) {
            val userId = jwtService.extractUserId(token)
            val username = jwtService.extractUsername(token)
            val isGuest = jwtService.extractIsGuest(token)
            logger.info("Usuario autenticado: $username, userId: $userId, isGuest: $isGuest")

            val authorities = listOf(
                SimpleGrantedAuthority(if (isGuest) "ROLE_GUEST" else "ROLE_USER")
            )

            val authentication: Authentication = UsernamePasswordAuthenticationToken(
                AuthenticatedUser(userId, username, isGuest),
                null,
                authorities
            )

            SecurityContextHolder.getContext().authentication = authentication
            logger.info("Autenticación establecida: ${SecurityContextHolder.getContext().authentication}")
        } else {
            logger.warn("Token no válido o ausente")
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader(AUTH_HEADER) ?: return null
        return if (header.startsWith(BEARER_PREFIX)) header.substring(BEARER_PREFIX.length) else null
    }
}

data class AuthenticatedUser(
    val userId: UUID,
    val username: String,
    val isGuest: Boolean
)