package ru.itmo.market.security

import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * JWT Authentication Filter for Reactive WebFlux
 * Extracts and validates JWT tokens from requests
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val token = extractToken(request)

        return if (token != null && jwtTokenProvider.validateToken(token)) {
            val authentication = createAuthentication(token)
            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        } else {
            chain.filter(exchange)
        }
    }

    /**
     * Extract JWT token from Authorization header
     */
    private fun extractToken(request: ServerHttpRequest): String? {
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        return jwtTokenProvider.extractTokenFromBearer(authHeader)
    }

    /**
     * Create Spring Security Authentication object from JWT token
     */
    private fun createAuthentication(token: String): UsernamePasswordAuthenticationToken {
        val userId = jwtTokenProvider.getUserIdFromToken(token)
        val username = jwtTokenProvider.getUsernameFromToken(token)
        val roles = jwtTokenProvider.getRolesFromToken(token)

        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }

        // Principal will be a map containing userId and username
        val principal = mapOf(
            "userId" to userId,
            "username" to username,
            "roles" to roles
        )

        return UsernamePasswordAuthenticationToken(principal, null, authorities)
    }
}
