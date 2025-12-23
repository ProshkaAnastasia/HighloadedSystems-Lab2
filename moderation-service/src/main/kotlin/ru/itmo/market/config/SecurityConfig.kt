package ru.itmo.market.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler
import ru.itmo.market.security.JwtAuthenticationFilter

/**
 * Spring Security Configuration for Moderation Service
 * Requires MODERATOR or ADMIN role for all moderation endpoints
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    /**
     * Configure security filter chain
     */
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .exceptionHandling { exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
            }
            .authorizeExchange { exchanges ->
                exchanges
                    // Swagger/OpenAPI endpoints - public
                    .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v1/api-docs/**", "/v3/api-docs/**", "/webjars/**").permitAll()

                    // Actuator endpoints - public
                    .pathMatchers("/actuator/health").permitAll()

                    // All moderation endpoints require MODERATOR or ADMIN role
                    .pathMatchers("/api/moderation/**").hasAnyRole("MODERATOR", "ADMIN")

                    // All other endpoints require authentication
                    .anyExchange().authenticated()
            }
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }
}
