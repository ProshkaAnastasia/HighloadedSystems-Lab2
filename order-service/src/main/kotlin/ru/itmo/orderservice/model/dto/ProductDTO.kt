package ru.itmo.orderservice.model.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductDTO(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val imageUrl: String?,
    val shopId: Long,
    val sellerId: Long,
    val status: String,
    val rejectionReason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
