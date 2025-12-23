package ru.itmo.userservice.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "bearer-jwt"
        return OpenAPI().apply {
            info = Info().apply {
                title = "ITMO Market - User Service API"
                description = "REST API для управления пользователями (Reactor + R2DBC). Включает регистрацию и аутентификацию с JWT."
                version = "1.0.0"
            }
            components = Components().apply {
                addSecuritySchemes(securitySchemeName, SecurityScheme().apply {
                    name = securitySchemeName
                    type = SecurityScheme.Type.HTTP
                    scheme = "bearer"
                    bearerFormat = "JWT"
                    description = "Введите JWT токен, полученный из /api/auth/login"
                })
            }
            addSecurityItem(SecurityRequirement().addList(securitySchemeName))
        }
    }
}
