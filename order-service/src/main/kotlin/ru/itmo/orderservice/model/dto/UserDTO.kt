package ru.itmo.orderservice.model.dto

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * UserDTO для маппинга
 */
data class UserDTO(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)