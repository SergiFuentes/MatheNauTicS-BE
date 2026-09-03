package com.mathenautics.backend.application.exception

class UsernameAlreadyExistsException : RuntimeException("Username already exists")
class EmailAlreadyExistsException : RuntimeException("Email already exists")
class InvalidCredentialsException : RuntimeException("Invalid username/email or password")
class UserNotFoundException : RuntimeException("User not found")
class GuestConversionException(message: String) : RuntimeException(message)
