package com.mathenautics.backend.application.exception

import com.mathenautics.backend.dto.ApiError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UsernameAlreadyExistsException::class, EmailAlreadyExistsException::class)
    fun registrationConflict(ex: RuntimeException): ResponseEntity<ApiError> =
        error(HttpStatus.CONFLICT, "REGISTRATION_FAILED", "Registration failed")

    @ExceptionHandler(InvalidCredentialsException::class)
    fun invalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ApiError> =
        error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.message!!)

    @ExceptionHandler(UserNotFoundException::class)
    fun userNotFound(ex: UserNotFoundException): ResponseEntity<ApiError> =
        error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.message!!)

    @ExceptionHandler(GuestConversionException::class)
    fun guestConversion(ex: GuestConversionException): ResponseEntity<ApiError> =
        error(HttpStatus.CONFLICT, "GUEST_CONVERSION_FAILED", ex.message!!)

    @ExceptionHandler(DuplicateGameSessionException::class)
    fun duplicateGameSession(ex: DuplicateGameSessionException): ResponseEntity<ApiError> =
        error(HttpStatus.CONFLICT, "DUPLICATE_SESSION", ex.message!!)

    @ExceptionHandler(IllegalArgumentException::class)
    fun invalidArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> =
        error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.message ?: "Invalid request")

    private fun error(status: HttpStatus, code: String, message: String): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(
            ApiError(status.value(), code, message, OffsetDateTime.now().toString())
        )
}