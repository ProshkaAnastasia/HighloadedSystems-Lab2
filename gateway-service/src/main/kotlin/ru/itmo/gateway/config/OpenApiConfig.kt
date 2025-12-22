package ru.itmo.gateway.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Gateway API")
                    .description("Unified API Gateway Documentation")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("API Support")
                            .url("http://localhost:8080")
                    )
            )
            .addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Development Server")
            )
    }
}