package ru.itmo.market.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import java.util.concurrent.CompletableFuture

@FeignClient(name = "product-service", url = "\${product-service.url:http://product-service:8082}")
interface ProductServiceClient {
    
    @GetMapping("/api/products/{id}")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    @TimeLimiter(name = "productService", fallbackMethod = "getProductFallback")
    fun getProductById(@PathVariable id: Long): ProductResponse
    
    @GetMapping("/api/products")
    @CircuitBreaker(name = "productService", fallbackMethod = "getPendingProductsFallback")
    @TimeLimiter(name = "productService", fallbackMethod = "getPendingProductsFallback")
    fun getPendingProducts(
        @RequestParam page: Int = 1,
        @RequestParam pageSize: Int = 20
    ): PaginatedResponse<ProductResponse>
    
    @PostMapping("/api/products/{id}/approve")
    @CircuitBreaker(name = "productService", fallbackMethod = "approveProductFallback")
    @TimeLimiter(name = "productService", fallbackMethod = "approveProductFallback")
    fun approveProduct(
        @PathVariable id: Long,
        @RequestParam moderatorId: Long
    ): ProductResponse
    
    @PostMapping("/api/products/{id}/reject")
    @CircuitBreaker(name = "productService", fallbackMethod = "rejectProductFallback")
    @TimeLimiter(name = "productService", fallbackMethod = "rejectProductFallback")
    fun rejectProduct(
        @PathVariable id: Long,
        @RequestParam moderatorId: Long,
        @RequestParam reason: String
    ): ProductResponse
    
    // Fallback методы
    fun getProductFallback(id: Long, e: Exception): ProductResponse {
        throw RuntimeException("Product Service недоступен. ID: $id", e)
    }
    
    fun getPendingProductsFallback(page: Int, pageSize: Int, e: Exception): PaginatedResponse<ProductResponse> {
        throw RuntimeException("Product Service недоступен при получении pending products", e)
    }
    
    fun approveProductFallback(id: Long, moderatorId: Long, e: Exception): ProductResponse {
        throw RuntimeException("Product Service недоступен при одобрении товара", e)
    }
    
    fun rejectProductFallback(id: Long, moderatorId: Long, reason: String, e: Exception): ProductResponse {
        throw RuntimeException("Product Service недоступен при отклонении товара", e)
    }
}
