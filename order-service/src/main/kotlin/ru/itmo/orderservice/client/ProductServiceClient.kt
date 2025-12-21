package ru.itmo.orderservice.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.orderservice.model.dto.ProductDTO
import java.util.concurrent.CompletableFuture

@FeignClient(
    name = "product-service",
    fallback = ProductServiceClientFallback::class,
    configuration = [FeignClientConfig::class]
)
interface ProductServiceClient {
    
    /**
     * Получить товар по ID
     */
    @GetMapping("/api/products/{productId}")
    @CircuitBreaker(
        name = "productService",
        fallbackMethod = "getProductByIdFallback"
    )
    @TimeLimiter(
        name = "productService",
        fallbackMethod = "getProductByIdFallback"
    )
    fun getProductById(@PathVariable("productId") productId: Long): ProductDTO
    
    /**
     * Проверить существование товара
     */
    @GetMapping("/api/products/{productId}/exists")
    fun productExists(@PathVariable("productId") productId: Long): Boolean
}

/**
 * Fallback реализация при недоступности сервиса
 */
@org.springframework.stereotype.Component
class ProductServiceClientFallback : ProductServiceClient {
    override fun getProductById(productId: Long): ProductDTO {
        throw RuntimeException("Product service is currently unavailable. Please try again later.")
    }
    
    override fun productExists(productId: Long): Boolean {
        throw RuntimeException("Product service is currently unavailable.")
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
