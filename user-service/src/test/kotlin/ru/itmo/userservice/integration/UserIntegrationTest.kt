package ru.itmo.userservice.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.model.dto.request.UpdateProfileRequest
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("User Service Integration Tests")
@org.springframework.test.context.ActiveProfiles("test")
class UserIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("testdb")
            withUsername("testuser")
            withPassword("testpass")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgresContainer.host}:${postgresContainer.getMappedPort(5432)}/${postgresContainer.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgresContainer.username }
            registry.add("spring.r2dbc.password") { postgresContainer.password }

            registry.add("spring.flyway.url") { postgresContainer.jdbcUrl }
            registry.add("spring.flyway.user") { postgresContainer.username }
            registry.add("spring.flyway.password") { postgresContainer.password }
        }
    }

    @BeforeEach
    fun setUp() {
        // Clean up database before each test
        userRoleRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }

    @Nested
    @DisplayName("User Registration Integration Tests")
    inner class RegistrationIntegrationTests {

        @Test
        @DisplayName("Should register new user end-to-end")
        fun `should register user end to end`() {
            // Given
            val registerRequest = RegisterRequest(
                username = "integrationuser",
                email = "integration@example.com",
                password = "password123",
                firstName = "Integration",
                lastName = "Test"
            )

            // When & Then
            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.username").isEqualTo("integrationuser")
                .jsonPath("$.email").isEqualTo("integration@example.com")
                .jsonPath("$.firstName").isEqualTo("Integration")
                .jsonPath("$.lastName").isEqualTo("Test")
                .jsonPath("$.roles").isArray
                .jsonPath("$.roles[0]").isEqualTo("USER")

            // Verify data in database
            val user = userRepository.findByUsername("integrationuser").block()
            assert(user != null)
            assert(user?.username == "integrationuser")
            assert(user?.email == "integration@example.com")

            // Verify role in database
            val roles = userRoleRepository.findByUserId(user!!.id!!).collectList().block()
            assert(roles?.size == 1)
            assert(roles?.get(0)?.role == "USER")
        }

        @Test
        @DisplayName("Should prevent duplicate username registration")
        fun `should prevent duplicate username`() {
            // Given - First registration
            val firstRequest = RegisterRequest(
                username = "duplicateuser",
                email = "first@example.com",
                password = "password123",
                firstName = "First",
                lastName = "User"
            )

            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(firstRequest)
                .exchange()
                .expectStatus().isCreated

            // When - Second registration with same username
            val secondRequest = RegisterRequest(
                username = "duplicateuser",
                email = "second@example.com",
                password = "password123",
                firstName = "Second",
                lastName = "User"
            )

            // Then
            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(secondRequest)
                .exchange()
                .expectStatus().isEqualTo(409)
        }

        @Test
        @DisplayName("Should prevent duplicate email registration")
        fun `should prevent duplicate email`() {
            // Given - First registration
            val firstRequest = RegisterRequest(
                username = "firstuser",
                email = "duplicate@example.com",
                password = "password123",
                firstName = "First",
                lastName = "User"
            )

            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(firstRequest)
                .exchange()
                .expectStatus().isCreated

            // When - Second registration with same email
            val secondRequest = RegisterRequest(
                username = "seconduser",
                email = "duplicate@example.com",
                password = "password123",
                firstName = "Second",
                lastName = "User"
            )

            // Then
            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(secondRequest)
                .exchange()
                .expectStatus().isEqualTo(409)
        }
    }

    @Nested
    @DisplayName("User Retrieval Integration Tests")
    inner class UserRetrievalIntegrationTests {

        @Test
        @DisplayName("Should retrieve user by ID end-to-end")
        fun `should retrieve user by id`() {
            // Given - Register a user first
            val registerRequest = RegisterRequest(
                username = "retrieveuser",
                email = "retrieve@example.com",
                password = "password123",
                firstName = "Retrieve",
                lastName = "Test"
            )

            val registrationResponse = webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .returnResult()

            // Extract userId from response
            val userId = extractUserIdFromResponse(registrationResponse.responseBody!!)

            // When & Then
            webTestClient.get()
                .uri("/api/users/$userId")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId)
                .jsonPath("$.username").isEqualTo("retrieveuser")
                .jsonPath("$.email").isEqualTo("retrieve@example.com")
        }

        @Test
        @DisplayName("Should retrieve user by username end-to-end")
        fun `should retrieve user by username`() {
            // Given - Register a user first
            val registerRequest = RegisterRequest(
                username = "findbyusername",
                email = "findby@example.com",
                password = "password123",
                firstName = "Find",
                lastName = "User"
            )

            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated

            // When & Then
            webTestClient.get()
                .uri("/api/users/username/findbyusername")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.username").isEqualTo("findbyusername")
                .jsonPath("$.email").isEqualTo("findby@example.com")
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        fun `should return 404 for non-existent user`() {
            // When & Then
            webTestClient.get()
                .uri("/api/users/99999")
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    @DisplayName("User Update Integration Tests")
    inner class UserUpdateIntegrationTests {

        @Test
        @DisplayName("Should update user profile end-to-end")
        fun `should update user profile`() {
            // Given - Register a user first
            val registerRequest = RegisterRequest(
                username = "updateuser",
                email = "update@example.com",
                password = "password123",
                firstName = "Old",
                lastName = "Name"
            )

            val registrationResponse = webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .returnResult()

            val userId = extractUserIdFromResponse(registrationResponse.responseBody!!)

            // When - Update profile
            val updateRequest = UpdateProfileRequest(
                email = "newemail@example.com",
                firstName = "New",
                lastName = "Name"
            )

            webTestClient.put()
                .uri("/api/users/me?userId=$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.email").isEqualTo("newemail@example.com")
                .jsonPath("$.firstName").isEqualTo("New")
                .jsonPath("$.lastName").isEqualTo("Name")

            // Verify changes in database
            val updatedUser = userRepository.findById(userId).block()
            assert(updatedUser?.email == "newemail@example.com")
            assert(updatedUser?.firstName == "New")
            assert(updatedUser?.lastName == "Name")
        }

        @Test
        @DisplayName("Should prevent email update to existing email")
        fun `should prevent email update to existing email`() {
            // Given - Register two users
            val firstUser = RegisterRequest(
                username = "firstuser",
                email = "first@example.com",
                password = "password123",
                firstName = "First",
                lastName = "User"
            )

            val secondUser = RegisterRequest(
                username = "seconduser",
                email = "second@example.com",
                password = "password123",
                firstName = "Second",
                lastName = "User"
            )

            webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(firstUser)
                .exchange()
                .expectStatus().isCreated

            val secondResponse = webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(secondUser)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .returnResult()

            val secondUserId = extractUserIdFromResponse(secondResponse.responseBody!!)

            // When - Try to update second user's email to first user's email
            val updateRequest = UpdateProfileRequest(
                email = "first@example.com",
                firstName = null,
                lastName = null
            )

            // Then
            webTestClient.put()
                .uri("/api/users/me?userId=$secondUserId")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isEqualTo(409)
        }
    }

    @Nested
    @DisplayName("User Deletion Integration Tests")
    inner class UserDeletionIntegrationTests {

        @Test
        @DisplayName("Should delete user and roles end-to-end")
        fun `should delete user and roles`() {
            // Given - Register a user first
            val registerRequest = RegisterRequest(
                username = "deleteuser",
                email = "delete@example.com",
                password = "password123",
                firstName = "Delete",
                lastName = "Me"
            )

            val registrationResponse = webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .returnResult()

            val userId = extractUserIdFromResponse(registrationResponse.responseBody!!)

            // Verify user exists
            val userBeforeDeletion = userRepository.findById(userId).block()
            assert(userBeforeDeletion != null)

            // When - Delete user
            webTestClient.delete()
                .uri("/api/users/$userId")
                .exchange()
                .expectStatus().isNoContent

            // Then - Verify user is deleted
            val userAfterDeletion = userRepository.findById(userId).block()
            assert(userAfterDeletion == null)

            // Verify roles are also deleted (cascade)
            val rolesAfterDeletion = userRoleRepository.findByUserId(userId).collectList().block()
            assert(rolesAfterDeletion?.isEmpty() == true)
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent user")
        fun `should return 404 when deleting non-existent user`() {
            // When & Then
            webTestClient.delete()
                .uri("/api/users/99999")
                .exchange()
                .expectStatus().isNotFound
        }
    }

    /**
     * Helper method to extract userId from JSON response
     */
    private fun extractUserIdFromResponse(responseBody: ByteArray): Long {
        val jsonString = String(responseBody)
        val idPattern = """"id"\s*:\s*(\d+)""".toRegex()
        val matchResult = idPattern.find(jsonString)
        return matchResult?.groupValues?.get(1)?.toLong()
            ?: throw IllegalStateException("Could not extract userId from response")
    }
}
