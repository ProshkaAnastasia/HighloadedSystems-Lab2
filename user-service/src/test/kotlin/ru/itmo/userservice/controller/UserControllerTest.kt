package ru.itmo.userservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.http.MediaType
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.model.dto.request.UpdateProfileRequest
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
@DisplayName("User Controller Tests")
class UserControllerTest {
    
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market_test")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }
    }
    
    @Autowired
    private lateinit var webTestClient: WebTestClient
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository
    
    @BeforeEach
    fun setUp() {
        userRoleRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }
    
    @Test
    @DisplayName("POST /api/users/register - Should register user")
    fun testRegisterUser() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        webTestClient.post()
            .uri("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.email").isEqualTo("test@example.com")
            .jsonPath("$.firstName").isEqualTo("Test")
            .jsonPath("$.lastName").isEqualTo("User")
    }
    
    @Test
    @DisplayName("POST /api/users/register - Should return 409 for duplicate username")
    fun testRegisterDuplicateUsername() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        // First registration
        webTestClient.post()
            .uri("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
        
        // Second registration with same username
        val request2 = request.copy(email = "test2@example.com")
        
        webTestClient.post()
            .uri("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()
            .expectStatus().isEqualTo(409)
    }
}
