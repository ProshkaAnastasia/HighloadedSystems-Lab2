package ru.itmo.userservice.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT Token Provider for generating and validating JWT tokens
 */
@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}") private val jwtSecret: String,
    @Value("\${app.jwt.expiration}") private val jwtExpirationMs: Long
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Generate JWT token for user
     *
     * @param userId User ID to include in token
     * @param username Username to include in token
     * @param roles List of user roles
     * @return Generated JWT token string
     */
    fun generateToken(userId: Long, username: String, roles: List<String>): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
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
