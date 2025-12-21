package ru.itmo.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {
    
    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://frontend:3000",
                "http://localhost:8080"
            )
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)
        
        return CorsWebFilter(source)
    }
}
