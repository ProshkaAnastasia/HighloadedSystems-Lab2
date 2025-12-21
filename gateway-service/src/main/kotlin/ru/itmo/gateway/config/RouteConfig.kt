package ru.itmo.gateway.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration
class RouteConfig {
    
    @Bean
    fun routes(builder: RouteLocatorBuilder): RouteLocator = builder.routes()
        // User Service routes
        .route("user-service") { r ->
            r.path("/api/users/**")
                .and()
                .method(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
                .filters { f ->
                    f.addRequestHeader("X-Service", "user-service")
                        .rewritePath("/api/users/?(?<path>.*)", "/api/users/$\\{path}")
                }
                .uri("lb://user-service")
        }
        // Product Service routes
        .route("product-service") { r ->
            r.path("/api/products/**", "/api/shops/**")
                .and()
                .method(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
                .filters { f ->
                    f.addRequestHeader("X-Service", "product-service")
                        .rewritePath("/api/(?<path>.*)", "/api/$\\{path}")
                }
                .uri("lb://product-service")
        }
        // Order Service routes
        .route("order-service") { r ->
            r.path("/api/orders/**", "/api/cart/**")
                .and()
                .method(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
                .filters { f ->
                    f.addRequestHeader("X-Service", "order-service")
                        .rewritePath("/api/(?<path>.*)", "/api/$\\{path}")
                }
                .uri("lb://order-service")
        }
        // Moderation Service routes
        .route("moderation-service") { r ->
            r.path("/api/moderation/**")
                .and()
                .method(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
                .filters { f ->
                    f.addRequestHeader("X-Service", "moderation-service")
                        .rewritePath("/api/moderation/?(?<path>.*)", "/api/moderation/$\\{path}")
                }
                .uri("lb://moderation-service")
        }
        // Actuator endpoints (health, metrics)
        .route("actuator") { r ->
            r.path("/actuator/**")
                .uri("http://localhost:8080")
        }
        // Swagger UI redirect
        .route("swagger-ui") { r ->
            r.path("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                .uri("http://localhost:8080")
        }
        .build()
}
