package ru.itmo.userservice.model.dto.response

/**
 * Login response DTO with JWT token
 */
data class LoginResponse(
    val token: String,
    val type: String = "Bearer",
    val userId: Long,
    val username: String,
    val roles: Set<String>
)
