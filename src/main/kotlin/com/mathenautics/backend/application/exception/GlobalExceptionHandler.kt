package com.mathenautics.backend.application.exception

import com.mathenautics.backend.dto.ApiError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(UsernameAlreadyExistsException::class)
    fun usernameExists(ex: UsernameAlreadyExistsException) = error(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", ex.message!!)

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun emailExists(ex: EmailAlreadyExistsException) = error(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", ex.message!!)

    @ExceptionHandler(InvalidCredentialsException::class)
    fun invalidCredentials(ex: InvalidCredentialsException) = error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.message!!)

    @ExceptionHandler(UserNotFoundException::class)
    fun userNotFound(ex: UserNotFoundException) = error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.message!!)

    @ExceptionHandler(GuestConversionException::class)
    fun guestConversion(ex: GuestConversionException) = error(HttpStatus.CONFLICT, "GUEST_CONVERSION_FAILED", ex.message!!)

    @ExceptionHandler(IllegalArgumentException::class)
    fun invalidArgument(ex: IllegalArgumentException) = error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.message ?: "Invalid request")

    private fun error(status: HttpStatus, code: String, message: String): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(
            ApiError(status.value(), code, message, OffsetDateTime.now().toString())
        )
}