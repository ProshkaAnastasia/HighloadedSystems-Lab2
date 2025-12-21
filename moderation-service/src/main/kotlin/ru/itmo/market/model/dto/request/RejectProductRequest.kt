package ru.itmo.market.model.dto.request

import jakarta.validation.constraints.NotBlank

data class RejectProductRequest(
    @field:NotBlank(message = "Reason is required")
    val reason: String
)