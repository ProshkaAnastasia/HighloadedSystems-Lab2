package ru.itmo.userservice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import ru.itmo.userservice.model.dto.request.LoginRequest
import ru.itmo.userservice.model.dto.response.ErrorResponse
import ru.itmo.userservice.model.dto.response.LoginResponse
import ru.itmo.userservice.service.AuthService

/**
 * Authentication Controller
 * Handles user authentication and JWT token generation
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and authorization API")
class AuthController(
    private val authService: AuthService
) {

    /**
     * Login endpoint
     * POST /api/auth/login
     *
     * @param loginRequest Login credentials (username and password)
     * @return LoginResponse with JWT token
     * @throws BadRequestException if credentials are invalid
     */
    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user and receive JWT token for subsequent requests"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login successful",
                content = [Content(schema = Schema(implementation = LoginResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid credentials",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun login(@Valid @RequestBody loginRequest: LoginRequest): Mono<ResponseEntity<LoginResponse>> {
        return authService.login(loginRequest)
            .map { ResponseEntity.ok(it) }
    }

    /**
     * Health check endpoint for auth service
     * GET /api/auth/health
     *
     * @return Health status
     */
    @GetMapping("/health")
    @Operation(
        summary = "Authentication service health check",
        description = "Check if authentication service is running"
    )
    fun health(): Mono<ResponseEntity<Map<String, String>>> {
        return Mono.just(
            ResponseEntity.ok(mapOf("status" to "UP", "service" to "auth"))
        )
    }
}
