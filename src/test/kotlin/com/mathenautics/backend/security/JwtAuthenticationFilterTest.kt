package com.mathenautics.backend.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtAuthenticationFilterTest {

    private val jwtService = mockk<JwtService>()
    private val filterChain = mockk<FilterChain>(relaxed = true)
    private val request = mockk<HttpServletRequest>()
    private val response = mockk<HttpServletResponse>(relaxed = true)

    private val filter = TestableJwtAuthenticationFilter(jwtService)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should continue filter chain without authentication for OPTIONS request`() {
        every { request.method } returns "OPTIONS"

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)

        verify(exactly = 1) {
            filterChain.doFilter(request, response)
        }

        verify(exactly = 0) {
            request.getHeader("Authorization")
        }
    }

    @Test
    fun `should continue filter chain without authentication when authorization header is missing`() {
        every { request.method } returns "GET"
        every { request.getHeader("Authorization") } returns null

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)

        verify(exactly = 1) {
            filterChain.doFilter(request, response)
        }

        verify(exactly = 1) {
            request.getHeader("Authorization")
        }

        verify(exactly = 0) {
            jwtService.isTokenValid(any())
        }
    }

    @Test
    fun `should continue filter chain without authentication when authorization header is not Bearer`() {
        every { request.method } returns "GET"
        every {
            request.getHeader("Authorization")
        } returns "Basic username:password"

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)

        verify(exactly = 1) {
            filterChain.doFilter(request, response)
        }

        verify(exactly = 1) {
            request.getHeader("Authorization")
        }

        verify(exactly = 0) {
            jwtService.isTokenValid(any())
        }
    }

    @Test
    fun `should continue filter chain without authentication when token is invalid`() {
        every { request.method } returns "GET"
        every {
            request.getHeader("Authorization")
        } returns "Bearer invalid.token"

        every {
            jwtService.isTokenValid("invalid.token")
        } returns false

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)

        verify(exactly = 1) {
            jwtService.isTokenValid("invalid.token")
        }

        verify(exactly = 0) {
            jwtService.extractUserId(any())
        }

        verify(exactly = 0) {
            jwtService.extractUsername(any())
        }

        verify(exactly = 0) {
            jwtService.extractIsGuest(any())
        }

        verify(exactly = 1) {
            filterChain.doFilter(request, response)
        }
    }

    @Test
    fun `should authenticate registered user with ROLE_USER when token is valid`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val token = "valid.user.token"

        every { request.method } returns "GET"
        every {
            request.getHeader("Authorization")
        } returns "Bearer $token"

        every {
            jwtService.isTokenValid(token)
        } returns true

        every {
            jwtService.extractUserId(token)
        } returns userId

        every {
            jwtService.extractUsername(token)
        } returns username

        every {
            jwtService.extractIsGuest(token)
        } returns false

        filter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder
            .getContext()
            .authentication

        assertEquals(
            AuthenticatedUser(
                userId = userId,
                username = username,
                isGuest = false
            ),
            authentication.principal
        )

        assertEquals(
            listOf("ROLE_USER"),
            authentication.authorities.map { it.authority }
        )

        assertEquals(true, authentication.isAuthenticated)

        verify(exactly = 1) {
            jwtService.isTokenValid(token)
        }

        verify(exactly = 1) {
            jwtService.extractUserId(token)
        }

        verify(exactly = 1) {
            jwtService.extractUsername(token)
        }

        verify(exactly = 1) {
            jwtService.extractIsGuest(token)
        }

        verify(exactly = 1) {
            filterChain.doFilter(request, response)
        }
    }

    @Test
    fun `should authenticate guest user with ROLE_GUEST when token is valid`() {
        val userId = UUID.randomUUID()
        val username = "guest"
        val token = "valid.guest.token"

        every { request.method } returns "GET"
        every {
            request.getHeader("Authorization")
        } returns "Bearer $token"

        every {
            jwtService.isTokenValid(token)
        } returns true

        every {
            jwtService.extractUserId(token)
        } returns userId

        every {
            jwtService.extractUsername(token)
        } returns username

        every {
            jwtService.extractIsGuest(token)
        } returns true

        filter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder
            .getContext()
            .authentication

        assertEquals(
            AuthenticatedUser(
                userId = userId,
                username = username,
                isGuest = true
            ),
            authentication.principal
        )

        assertEquals(
            listOf("ROLE_GUEST"),
            authentication.authorities.map { it.authority }
        )

        assertEquals(true, authentication.isAuthenticated)

        verify(exactly = 1) {
            jwtService.isTokenValid(token)
        }

        verify(exactly = 1) {
            jwtService.extractUserId(token)
        }

        verify(exactly = 1) {
            jwtService.extractUsername(token)
        }

        verify(exactly = 1) {
            jwtService.extractIsGuest(token)
        }

        verify(exactly = 1) {
            filterChain.doFilter(request, response)
        }
    }

    private class TestableJwtAuthenticationFilter(
        jwtService: JwtService
    ) : JwtAuthenticationFilter(jwtService) {

        fun doFilter(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            doFilterInternal(request, response, filterChain)
        }
    }
}