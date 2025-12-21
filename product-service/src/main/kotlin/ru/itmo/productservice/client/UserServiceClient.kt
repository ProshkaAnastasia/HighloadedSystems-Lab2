package ru.itmo.productservice.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.productservice.model.dto.response.UserResponse
import java.util.concurrent.CompletableFuture

@FeignClient(
    name = "user-service",
    fallback = UserServiceClientFallback::class,
    configuration = [FeignClientConfig::class]
)
interface UserServiceClient {
    
    /**
     * Получить пользователя по ID
     * ВНУТРЕННИЙ ЭНДПОИНТ
     */
    @GetMapping("/api/users/{userId}")
    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "getUserByIdFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "getUserByIdFallback"
    )
    fun getUserById(@PathVariable("userId") userId: Long): UserResponse
}

/**
 * Fallback реализация при недоступности сервиса
 */
@org.springframework.stereotype.Component
class UserServiceClientFallback : UserServiceClient {
    override fun getUserById(userId: Long): UserResponse {
        throw RuntimeException("User service is currently unavailable. Please try again later.")
    }
}

/**
 * Конфигурация Feign Client
 */
@org.springframework.context.annotation.Configuration
class FeignClientConfig {
    @org.springframework.context.annotation.Bean
    fun feignClientBuilder(): feign.Client {
        return feign.Client.Default(null, null)
    }
}
