package ru.itmo.market.model.dto.response

import java.time.LocalDateTime

data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: LocalDateTime
)