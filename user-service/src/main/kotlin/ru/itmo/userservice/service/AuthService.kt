package ru.itmo.userservice.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.itmo.userservice.model.dto.request.LoginRequest
import ru.itmo.userservice.model.dto.response.LoginResponse
import ru.itmo.userservice.repository.UserRoleRepository
import ru.itmo.userservice.security.JwtTokenProvider

/**
 * Authentication service for handling login operations
 */
@Service
class AuthService(
    private val userService: UserService,
    private val userRoleRepository: UserRoleRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * Authenticate user and generate JWT token
     *
     * @param loginRequest Login credentials
     * @return LoginResponse with JWT token
     */
    fun login(loginRequest: LoginRequest): Mono<LoginResponse> {
        return userService.authenticate(loginRequest.username, loginRequest.password)
            .flatMap { user ->
                val userId = user.id ?: throw IllegalStateException("User ID cannot be null")

                // Get user roles
                userRoleRepository.findByUserId(userId)
                    .map { it.role }
                    .collectList()
                    .map { roles ->
                        // Generate JWT token
                        val token = jwtTokenProvider.generateToken(
                            userId = userId,
                            username = user.username,
                            roles = roles
                        )

                        // Return login response
                        LoginResponse(
                            token = token,
                            userId = userId,
                            username = user.username,
                            roles = roles.toSet()
                        )
                    }
            }
    }
}
