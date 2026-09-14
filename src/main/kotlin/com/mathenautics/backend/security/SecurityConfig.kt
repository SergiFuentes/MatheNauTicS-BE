package com.mathenautics.backend.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    @Value("\${cors.allowed-origins}") private val corsAllowedOrigins: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling {
                it
                    .authenticationEntryPoint(
                        HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                    )
                    .accessDeniedHandler { _, response, _ ->
                        response.sendError(HttpStatus.FORBIDDEN.value())
                    }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                        "/",
                        "/health",
                        "/api/v1/auth/login",
                        "/api/v1/users",
                        "/api/v1/users/*/convert",
                        "/api/v1/games/leaderboard"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )
            .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val origins = corsAllowedOrigins
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val configuration = CorsConfiguration().apply {
            allowedOrigins = origins
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    /**
     * Provides a custom UserDetailsService to prevent Spring Boot from auto-configuring
     * an InMemoryUserDetailsManager and generating a default development password.
     *
     * Authentication is performed by the application's JWT-based security filter.
     */
    @Bean
    fun userDetailsService(): UserDetailsService {
        return UserDetailsService { _ ->
            throw UsernameNotFoundException("Authentication via JWT only")
        }
    }
}