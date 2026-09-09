package com.mathenautics.backend.security

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
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
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
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "http://localhost:5500",
                "http://127.0.0.1:5500",
                "http://192.168.1.49:5500",
                "http://192.168.0.16:5500",
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://172.21.224.1:5500",
                "http://192.168.61.55:5500"
            )
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