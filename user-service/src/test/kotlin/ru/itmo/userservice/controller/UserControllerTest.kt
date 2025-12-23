package ru.itmo.userservice.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import ru.itmo.userservice.exception.BadRequestException
import ru.itmo.userservice.exception.ConflictException
import ru.itmo.userservice.exception.ResourceNotFoundException
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.model.dto.request.UpdateProfileRequest
import ru.itmo.userservice.model.dto.response.UserResponse
import ru.itmo.userservice.service.UserService
import java.time.LocalDateTime

@WebFluxTest(UserController::class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var userService: UserService

    private lateinit var sampleUserResponse: UserResponse

    @BeforeEach
    fun setUp() {
        sampleUserResponse = UserResponse(
            id = 1L,
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            roles = setOf("USER"),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Nested
    @DisplayName("POST /api/users/register")
    inner class RegisterEndpointTests {

        @Test
        @DisplayName("Should register user successfully and return 201 Created")
        fun `should register user successfully`() {
            // Given
            val registerRequest = RegisterRequest(
                username = "newuser",
                email = "newuser@example.com",
                password = "password123",
                firstName = "New",
                lastName = "User"
            )

            whenever(userService.register(any<RegisterRequest>()))
                .thenReturn(Mono.just(sampleUserResponse))

            // When & Then
            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.username").isEqualTo("testuser")
                .jsonPath("$.email").isEqualTo("test@example.com")
                .jsonPath("$.roles").isArray
                .jsonPath("$.roles[0]").isEqualTo("USER")

            verify(userService).register(any<RegisterRequest>())
        }

        @Test
        @DisplayName("Should return 409 Conflict when username already exists")
        fun `should return 409 when username exists`() {
            // Given
            val registerRequest = RegisterRequest(
                username = "existinguser",
                email = "test@example.com",
                password = "password123",
                firstName = "Test",
                lastName = "User"
            )

            whenever(userService.register(any<RegisterRequest>()))
                .thenReturn(Mono.error(ConflictException("Username already taken: existinguser")))

            // When & Then
            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isEqualTo(409)

            verify(userService).register(any<RegisterRequest>())
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password is too short")
        fun `should return 400 when password too short`() {
            // Given
            val registerRequest = RegisterRequest(
                username = "testuser",
                email = "test@example.com",
                password = "short",
                firstName = "Test",
                lastName = "User"
            )

            whenever(userService.register(any<RegisterRequest>()))
                .thenReturn(Mono.error(BadRequestException("Password must be at least 8 characters")))

            // When & Then
            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isBadRequest

            verify(userService).register(any<RegisterRequest>())
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email format is invalid")
        fun `should return 400 when email invalid`() {
            // Given - Missing email field should fail validation
            val invalidRequest = mapOf(
                "username" to "testuser",
                "email" to "invalid-email",
                "password" to "password123",
                "firstName" to "Test",
                "lastName" to "User"
            )

            // When & Then
            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("GET /api/users/me")
    inner class GetCurrentUserEndpointTests {

        @Test
        @DisplayName("Should get current user successfully")
        fun `should get current user successfully`() {
            // Given
            val userId = 1L

            whenever(userService.getCurrentUser(userId))
                .thenReturn(Mono.just(sampleUserResponse))

            // When & Then
            webTestClient.get()
                .uri("/api/users/me?userId=$userId")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.username").isEqualTo("testuser")

            verify(userService).getCurrentUser(userId)
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        fun `should return 404 when user not found`() {
            // Given
            val userId = 999L

            whenever(userService.getCurrentUser(userId))
                .thenReturn(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))

            // When & Then
            webTestClient.get()
                .uri("/api/users/me?userId=$userId")
                .exchange()
                .expectStatus().isNotFound

            verify(userService).getCurrentUser(userId)
        }

        @Test
        @DisplayName("Should return 400 when userId is invalid")
        fun `should return 400 when userId invalid`() {
            // Given
            val invalidUserId = -1L

            whenever(userService.getCurrentUser(invalidUserId))
                .thenReturn(Mono.error(BadRequestException("Invalid user ID: $invalidUserId")))

            // When & Then
            webTestClient.get()
                .uri("/api/users/me?userId=$invalidUserId")
                .exchange()
                .expectStatus().isBadRequest

            verify(userService).getCurrentUser(invalidUserId)
        }
    }

    @Nested
    @DisplayName("GET /api/users/{userId}")
    inner class GetUserByIdEndpointTests {

        @Test
        @DisplayName("Should get user by ID successfully")
        fun `should get user by id successfully`() {
            // Given
            val userId = 1L

            whenever(userService.getUserById(userId))
                .thenReturn(Mono.just(sampleUserResponse))

            // When & Then
            webTestClient.get()
                .uri("/api/users/$userId")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.username").isEqualTo("testuser")
                .jsonPath("$.email").isEqualTo("test@example.com")

            verify(userService).getUserById(userId)
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        fun `should return 404 when user not found`() {
            // Given
            val userId = 999L

            whenever(userService.getUserById(userId))
                .thenReturn(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))

            // When & Then
            webTestClient.get()
                .uri("/api/users/$userId")
                .exchange()
                .expectStatus().isNotFound

            verify(userService).getUserById(userId)
        }
    }

    @Nested
    @DisplayName("GET /api/users/username/{username}")
    inner class GetUserByUsernameEndpointTests {

        @Test
        @DisplayName("Should get user by username successfully")
        fun `should get user by username successfully`() {
            // Given
            val username = "testuser"

            whenever(userService.getUserByUsername(username))
                .thenReturn(Mono.just(sampleUserResponse))

            // When & Then
            webTestClient.get()
                .uri("/api/users/username/$username")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.username").isEqualTo("testuser")

            verify(userService).getUserByUsername(username)
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        fun `should return 404 when username not found`() {
            // Given
            val username = "nonexistent"

            whenever(userService.getUserByUsername(username))
                .thenReturn(Mono.error(ResourceNotFoundException("User not found with username: $username")))

            // When & Then
            webTestClient.get()
                .uri("/api/users/username/$username")
                .exchange()
                .expectStatus().isNotFound

            verify(userService).getUserByUsername(username)
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me")
    inner class UpdateProfileEndpointTests {

        @Test
        @DisplayName("Should update profile successfully")
        fun `should update profile successfully`() {
            // Given
            val userId = 1L
            val updateRequest = UpdateProfileRequest(
                email = "newemail@example.com",
                firstName = "Updated",
                lastName = "Name"
            )

            val updatedResponse = sampleUserResponse.copy(
                email = "newemail@example.com",
                firstName = "Updated",
                lastName = "Name"
            )

            whenever(userService.updateProfile(eq(userId), any<UpdateProfileRequest>()))
                .thenReturn(Mono.just(updatedResponse))

            // When & Then
            webTestClient.put()
                .uri("/api/users/me?userId=$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.email").isEqualTo("newemail@example.com")
                .jsonPath("$.firstName").isEqualTo("Updated")
                .jsonPath("$.lastName").isEqualTo("Name")

            verify(userService).updateProfile(eq(userId), any<UpdateProfileRequest>())
        }

        @Test
        @DisplayName("Should return 409 when email already exists")
        fun `should return 409 when email already exists`() {
            // Given
            val userId = 1L
            val updateRequest = UpdateProfileRequest(
                email = "taken@example.com",
                firstName = null,
                lastName = null
            )

            whenever(userService.updateProfile(eq(userId), any<UpdateProfileRequest>()))
                .thenReturn(Mono.error(ConflictException("Email already registered: taken@example.com")))

            // When & Then
            webTestClient.put()
                .uri("/api/users/me?userId=$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isEqualTo(409)

            verify(userService).updateProfile(eq(userId), any<UpdateProfileRequest>())
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        fun `should return 404 when user not found`() {
            // Given
            val userId = 999L
            val updateRequest = UpdateProfileRequest(
                email = "test@example.com",
                firstName = null,
                lastName = null
            )

            whenever(userService.updateProfile(eq(userId), any<UpdateProfileRequest>()))
                .thenReturn(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))

            // When & Then
            webTestClient.put()
                .uri("/api/users/me?userId=$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound

            verify(userService).updateProfile(eq(userId), any<UpdateProfileRequest>())
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{userId}")
    inner class DeleteUserEndpointTests {

        @Test
        @DisplayName("Should delete user successfully")
        fun `should delete user successfully`() {
            // Given
            val userId = 1L

            whenever(userService.deleteUser(userId))
                .thenReturn(Mono.empty())

            // When & Then
            webTestClient.delete()
                .uri("/api/users/$userId")
                .exchange()
                .expectStatus().isNoContent

            verify(userService).deleteUser(userId)
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        fun `should return 404 when user not found`() {
            // Given
            val userId = 999L

            whenever(userService.deleteUser(userId))
                .thenReturn(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))

            // When & Then
            webTestClient.delete()
                .uri("/api/users/$userId")
                .exchange()
                .expectStatus().isNotFound

            verify(userService).deleteUser(userId)
        }

        @Test
        @DisplayName("Should return 400 when userId is invalid")
        fun `should return 400 when userId invalid`() {
            // Given
            val invalidUserId = -1L

            whenever(userService.deleteUser(invalidUserId))
                .thenReturn(Mono.error(BadRequestException("Invalid user ID: $invalidUserId")))

            // When & Then
            webTestClient.delete()
                .uri("/api/users/$invalidUserId")
                .exchange()
                .expectStatus().isBadRequest

            verify(userService).deleteUser(invalidUserId)
        }
    }
}
