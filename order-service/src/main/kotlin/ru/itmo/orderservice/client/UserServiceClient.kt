package ru.itmo.orderservice.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.orderservice.model.dto.UserDTO
import java.time.LocalDateTime

@FeignClient(name = "user-service")
interface UserServiceClient {
    
    /**
     * Получить пользователя по ID
     */
    @GetMapping("/api/users/{userId}")
    fun getUserById(@PathVariable("userId") userId: Long): UserDTO
}

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
