package ru.itmo.market.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT Token Provider for validating JWT tokens
 * Note: This service only validates tokens, it does not generate them
 * Token generation happens in user-service
 */
@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}") private val jwtSecret: String
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Extract user ID from JWT token
     *
     * @param token JWT token string
     * @return User ID
     */
    fun getUserIdFromToken(token: String): Long {
        val claims = getAllClaimsFromToken(token)
        return claims.subject.toLong()
    }

    /**
     * Extract username from JWT token
     *
     * @param token JWT token string
     * @return Username
     */
    fun getUsernameFromToken(token: String): String {
        val claims = getAllClaimsFromToken(token)
        return claims["username"] as String
    }

    /**
     * Extract roles from JWT token
     *
     * @param token JWT token string
     * @return List of roles
     */
    @Suppress("UNCHECKED_CAST")
    fun getRolesFromToken(token: String): List<String> {
        val claims = getAllClaimsFromToken(token)
        return claims["roles"] as? List<String> ?: emptyList()
    }

    /**
     * Validate JWT token
     *
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = getAllClaimsFromToken(token)
            !isTokenExpired(claims)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get all claims from JWT token
     *
     * @param token JWT token string
     * @return Claims object
     */
    private fun getAllClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /**
     * Check if token is expired
     *
     * @param claims Token claims
     * @return true if token is expired, false otherwise
     */
    private fun isTokenExpired(claims: Claims): Boolean {
        val expiration = claims.expiration
        return expiration.before(Date())
    }

    /**
     * Extract token from Bearer header
     *
     * @param bearerToken Full Authorization header value (e.g., "Bearer token")
     * @return Extracted token or null if invalid format
     */
    fun extractTokenFromBearer(bearerToken: String?): String? {
        if (bearerToken.isNullOrBlank()) {
            return null
        }

        return if (bearerToken.startsWith("Bearer ", ignoreCase = true)) {
            bearerToken.substring(7).trim()
        } else {
            null
        }
    }
}
