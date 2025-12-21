package ru.itmo.gateway.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    
    @Bean
    fun customOpenAPI(
        @Value("\${springdoc.api-docs.title:ITMO Market API}") title: String,
        @Value("\${springdoc.api-docs.description:Unified API Documentation}") description: String,
        @Value("\${springdoc.api-docs.version:1.0.0}") version: String
    ): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title(title)
                    .description(description)
                    .version(version)
            )
            .addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Local Gateway")
            )
    }
}
