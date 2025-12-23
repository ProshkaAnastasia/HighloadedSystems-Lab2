package ru.itmo.market.security

import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.core.publisher.Mono

/**
 * Security utilities for extracting user information from JWT token
 */
object SecurityUtils {

    /**
     * Extract user ID from the current security context (JWT token)
     *
     * @return Mono of user ID
     */
    fun getCurrentUserId(): Mono<Long> {
        return ReactiveSecurityContextHolder.getContext()
            .map { context ->
                val authentication = context.authentication
                val principal = authentication.principal as? Map<*, *>
                    ?: throw IllegalStateException("Invalid authentication principal")

                principal["userId"] as? Long
                    ?: throw IllegalStateException("User ID not found in token")
            }
    }

    /**
     * Extract username from the current security context (JWT token)
     *
     * @return Mono of username
     */
    fun getCurrentUsername(): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .map { context ->
                val authentication = context.authentication
                val principal = authentication.principal as? Map<*, *>
                    ?: throw IllegalStateException("Invalid authentication principal")

                principal["username"] as? String
                    ?: throw IllegalStateException("Username not found in token")
            }
    }

    /**
     * Extract roles from the current security context (JWT token)
     *
     * @return Mono of roles list
     */
    @Suppress("UNCHECKED_CAST")
    fun getCurrentUserRoles(): Mono<List<String>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { context ->
                val authentication = context.authentication
                val principal = authentication.principal as? Map<*, *>
                    ?: throw IllegalStateException("Invalid authentication principal")

                principal["roles"] as? List<String>
                    ?: emptyList()
            }
    }
}
