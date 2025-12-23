package ru.itmo.userservice.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import reactor.test.publisher.TestPublisher
import ru.itmo.userservice.exception.BadRequestException
import ru.itmo.userservice.exception.ConflictException
import ru.itmo.userservice.exception.ResourceNotFoundException
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.model.dto.request.UpdateProfileRequest
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository
import java.time.Duration

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("User Service Tests")
class UserServiceTest {
    
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market_test")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }
    }
    
    @Autowired
    private lateinit var userService: UserService
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository
    
    @BeforeEach
    fun setUp() {
        // Очистка данных перед каждым тестом
        userRoleRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }
    
    @Test
    @DisplayName("Should register user successfully")
    fun testRegisterUserSuccess() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        StepVerifier.create(userService.register(request))
            .assertNext { response ->
                assert(response.username == "testuser")
                assert(response.email == "test@example.com")
                assert(response.firstName == "Test")
                assert(response.lastName == "User")
                assert(response.roles.contains("USER"))
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }
    
    @Test
    @DisplayName("Should fail registration with duplicate username")
    fun testRegisterDuplicateUsername() {
        val request1 = RegisterRequest(
            username = "testuser",
            email = "test1@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        userService.register(request1).block()
        
        val request2 = RegisterRequest(
            username = "testuser",
            email = "test2@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        StepVerifier.create(userService.register(request2))
            .expectError(ConflictException::class.java)
            .verify(Duration.ofSeconds(5))
    }
    
    @Test
    @DisplayName("Should fail registration with duplicate email")
    fun testRegisterDuplicateEmail() {
        val request1 = RegisterRequest(
            username = "testuser1",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        userService.register(request1).block()
        
        val request2 = RegisterRequest(
            username = "testuser2",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        StepVerifier.create(userService.register(request2))
            .expectError(ConflictException::class.java)
            .verify(Duration.ofSeconds(5))
    }
    
    @Test
    @DisplayName("Should get user by ID")
    fun testGetUserById() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        val registeredUser = userService.register(registerRequest).block()!!
        
        StepVerifier.create(userService.getUserById(registeredUser.id))
            .assertNext { response ->
                assert(response.id == registeredUser.id)
                assert(response.username == "testuser")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }
    
    @Test
    @DisplayName("Should fail getting non-existent user")
    fun testGetNonExistentUser() {
        StepVerifier.create(userService.getUserById(999L))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(5))
    }
    
    @Test
    @DisplayName("Should update user profile")
    fun testUpdateProfile() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        val registeredUser = userService.register(registerRequest).block()!!
        
        val updateRequest = UpdateProfileRequest(
            email = "newemail@example.com",
            firstName = "Updated",
            lastName = "Name"
        )
        
        StepVerifier.create(userService.updateProfile(registeredUser.id, updateRequest))
            .assertNext { response ->
                assert(response.email == "newemail@example.com")
                assert(response.firstName == "Updated")
                assert(response.lastName == "Name")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }
    
    @Test
    @DisplayName("Should delete user")
    fun testDeleteUser() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        
        val registeredUser = userService.register(registerRequest).block()!!
        
        StepVerifier.create(userService.deleteUser(registeredUser.id))
            .expectComplete()
            .verify(Duration.ofSeconds(5))
        
        StepVerifier.create(userService.getUserById(registeredUser.id))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(5))
    }
}
