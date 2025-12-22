package ru.itmo.productservice.client

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.productservice.model.dto.response.UserResponse
import ru.itmo.productservice.exception.ServiceUnavailableException
import ru.itmo.productservice.exception.ResourceNotFoundException
import java.util.concurrent.CompletableFuture
import feign.Response
import feign.codec.ErrorDecoder

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
    fun getUserById(@PathVariable("userId") userId: Long): UserResponse
}

/**
 * Fallback реализация при недоступности сервиса
 */
@Component
class UserServiceClientFallback : UserServiceClient {
    override fun getUserById(userId: Long): UserResponse {
        throw ServiceUnavailableException("User service is currently unavailable. Please try again later.")
    }
}

/**
 * Конфигурация Feign Client
 */
@Configuration
class FeignClientConfig {
    @Bean
    fun feignClientBuilder(): feign.Client {
        return feign.Client.Default(null, null)
    }
    
    @Bean
    fun errorDecoder(): ErrorDecoder {
        return ErrorDecoder { _, response ->
            when (response.status()) {  // ← метод, не свойство
                404 -> ResourceNotFoundException("User not found")
                500, 502, 503 -> ServiceUnavailableException("User service is currently unavailable. Please try again later.")
                else -> RuntimeException("HTTP ${response.status()}")
            }
        }
    }
}
