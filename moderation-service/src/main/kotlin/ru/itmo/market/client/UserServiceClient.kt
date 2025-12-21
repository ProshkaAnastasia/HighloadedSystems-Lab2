package ru.itmo.market.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.market.model.dto.response.UserResponse

@FeignClient(name = "user-service", url = "\${user-service.url:http://user-service:8081}")
interface UserServiceClient {
    
    @GetMapping("/api/users/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    @TimeLimiter(name = "userService", fallbackMethod = "getUserFallback")
    fun getUserById(@PathVariable id: Long): UserResponse
    
    fun getUserFallback(id: Long, e: Exception): UserResponse {
        throw RuntimeException("User Service недоступен. ID: $id", e)
    }
}
