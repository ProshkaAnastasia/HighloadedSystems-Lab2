package ru.itmo.userservice.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import ru.itmo.userservice.exception.BadRequestException
import ru.itmo.userservice.exception.ConflictException
import ru.itmo.userservice.exception.ResourceNotFoundException
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.model.dto.request.UpdateProfileRequest
import ru.itmo.userservice.model.entity.User
import ru.itmo.userservice.model.entity.UserRoleEntity
import ru.itmo.userservice.model.enums.UserRole
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository
import java.time.LocalDateTime

@DisplayName("UserService Unit Tests")
class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userRoleRepository: UserRoleRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        userRoleRepository = mock()
        userService = UserService(userRepository, userRoleRepository)
    }

    @Nested
    @DisplayName("User Registration Tests")
    inner class RegistrationTests {

        @Test
        @DisplayName("Should successfully register a new user with USER role")
        fun `should register new user successfully`() {
            // Given
            val registerRequest = RegisterRequest(
                username = "testuser",
                email = "test@example.com",
                password = "password123",
                firstName = "Test",
                lastName = "User"
            )

            val savedUser = User(
                id = 1L,
                username = "testuser",
                email = "test@example.com",
                password = "hashedPassword",
                firstName = "Test",
                lastName = "User",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val savedRole = UserRoleEntity(
                id = 1L,
                userId = 1L,
                role = UserRole.USER.name
            )

            whenever(userRepository.existsByUsername(registerRequest.username))
                .thenReturn(Mono.just(false))
            whenever(userRepository.existsByEmail(registerRequest.email))
                .thenReturn(Mono.just(false))
            whenever(userRepository.save(any<User>()))
                .thenReturn(Mono.just(savedUser))
            whenever(userRoleRepository.save(any<UserRoleEntity>()))
                .thenReturn(Mono.just(savedRole))
            whenever(userRoleRepository.findByUserId(1L))
                .thenReturn(Flux.just(savedRole))

            // When & Then
            StepVerifier.create(userService.register(registerRequest))
                .expectNextMatches { response ->
                    response.id == 1L &&
                    response.username == "testuser" &&
                    response.email == "test@example.com" &&
                    response.firstName == "Test" &&
                    response.lastName == "User" &&
                    response.roles.contains(UserRole.USER.name)
                }
                .verifyComplete()

            // Verify interactions
            verify(userRepository).existsByUsername("testuser")
            verify(userRepository).existsByEmail("test@example.com")
            verify(userRepository).save(any<User>())
            verify(userRoleRepository).save(any<UserRoleEntity>())
        }

        @Test
        @DisplayName("Should throw ConflictException when username already exists")
        fun `should fail registration when username exists`() {
            // Given
            val registerRequest = RegisterRequest(
                username = "existinguser",
                email = "test@example.com",
                password = "password123",
                firstName = "Test",
                lastName = "User"
            )

            whenever(userRepository.existsByUsername("existinguser"))
                .thenReturn(Mono.just(true))

            // When & Then
            StepVerifier.create(userService.register(registerRequest))
                .expectErrorMatches { error ->
                    error is ConflictException &&
                    error.message == "Username already taken: existinguser"
                }
                .verify()

            verify(userRepository).existsByUsername("existinguser")
            verify(userRepository, never()).save(any<User>())
        }

        @Test
        @DisplayName("Should throw ConflictException when email already exists")
        fun `should fail registration when email exists`() {
            // Given
            val registerRequest = RegisterRequest(
                username = "newuser",
                email = "existing@example.com",
                password = "password123",
                firstName = "Test",
                lastName = "User"
            )

            whenever(userRepository.existsByUsername("newuser"))
                .thenReturn(Mono.just(false))
            whenever(userRepository.existsByEmail("existing@example.com"))
                .thenReturn(Mono.just(true))

            // When & Then
            StepVerifier.create(userService.register(registerRequest))
                .expectErrorMatches { error ->
                    error is ConflictException &&
                    error.message == "Email already registered: existing@example.com"
                }
                .verify()

            verify(userRepository).existsByUsername("newuser")
            verify(userRepository).existsByEmail("existing@example.com")
            verify(userRepository, never()).save(any<User>())
        }

        @Test
        @DisplayName("Should throw BadRequestException when username is blank")
        fun `should fail registration when username is blank`() {
            // Given
            val registerRequest = RegisterRequest(
                username = "",
                email = "test@example.com",
                password = "password123",
                firstName = "Test",
                lastName = "User"
            )

            // When & Then
            StepVerifier.create(userService.register(registerRequest))
                .expectErrorMatches { error ->
                    error is BadRequestException &&
                    error.message == "Username and email cannot be empty"
                }
                .verify()

            verify(userRepository, never()).save(any<User>())
        }

        @Test
        @DisplayName("Should throw BadRequestException when password is too short")
        fun `should fail registration when password is too short`() {
            // Given
            val registerRequest = RegisterRequest(
                username = "testuser",
                email = "test@example.com",
                password = "short",
                firstName = "Test",
                lastName = "User"
            )

            // When & Then
            StepVerifier.create(userService.register(registerRequest))
                .expectErrorMatches { error ->
                    error is BadRequestException &&
                    error.message == "Password must be at least 8 characters"
                }
                .verify()

            verify(userRepository, never()).save(any<User>())
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    inner class GetUserTests {

        @Test
        @DisplayName("Should get user by ID successfully")
        fun `should get user by id successfully`() {
            // Given
            val userId = 1L
            val user = User(
                id = userId,
                username = "testuser",
                email = "test@example.com",
                password = "hashedPassword",
                firstName = "Test",
                lastName = "User",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val userRole = UserRoleEntity(
                id = 1L,
                userId = userId,
                role = UserRole.USER.name
            )

            whenever(userRepository.findById(any<Long>()))
                .thenReturn(Mono.just(user))
            whenever(userRoleRepository.findByUserId(userId))
                .thenReturn(Flux.just(userRole))

            // When & Then
            StepVerifier.create(userService.getUserById(userId))
                .expectNextMatches { response ->
                    response.id == userId &&
                    response.username == "testuser" &&
                    response.email == "test@example.com"
                }
                .verifyComplete()

            verify(userRepository).findById(any<Long>())
            verify(userRoleRepository).findByUserId(userId)
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        fun `should throw exception when user not found`() {
            // Given
            val userId = 999L

            whenever(userRepository.findById(any<Long>()))
                .thenReturn(Mono.empty())

            // When & Then
            StepVerifier.create(userService.getUserById(userId))
                .expectErrorMatches { error ->
                    error is ResourceNotFoundException &&
                    error.message == "User not found with ID: $userId"
                }
                .verify()

            verify(userRepository).findById(any<Long>())
        }

        @Test
        @DisplayName("Should throw BadRequestException when userId is invalid")
        fun `should throw exception when userId is invalid`() {
            // Given
            val invalidUserId = -1L

            // When & Then
            StepVerifier.create(userService.getUserById(invalidUserId))
                .expectErrorMatches { error ->
                    error is BadRequestException &&
                    error.message == "Invalid user ID: $invalidUserId"
                }
                .verify()

            verify(userRepository, never()).findById(any<Long>())
        }

        @Test
        @DisplayName("Should get user by username successfully")
        fun `should get user by username successfully`() {
            // Given
            val username = "testuser"
            val user = User(
                id = 1L,
                username = username,
                email = "test@example.com",
                password = "hashedPassword",
                firstName = "Test",
                lastName = "User",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val userRole = UserRoleEntity(
                id = 1L,
                userId = 1L,
                role = UserRole.USER.name
            )

            whenever(userRepository.findByUsername(username))
                .thenReturn(Mono.just(user))
            whenever(userRoleRepository.findByUserId(1L))
                .thenReturn(Flux.just(userRole))

            // When & Then
            StepVerifier.create(userService.getUserByUsername(username))
                .expectNextMatches { response ->
                    response.username == username &&
                    response.email == "test@example.com"
                }
                .verifyComplete()

            verify(userRepository).findByUsername(username)
        }

        @Test
        @DisplayName("Should throw BadRequestException when username is blank")
        fun `should throw exception when username is blank`() {
            // Given
            val blankUsername = ""

            // When & Then
            StepVerifier.create(userService.getUserByUsername(blankUsername))
                .expectErrorMatches { error ->
                    error is BadRequestException &&
                    error.message == "Username cannot be empty"
                }
                .verify()

            verify(userRepository, never()).findByUsername(any())
        }
    }

    @Nested
    @DisplayName("Update Profile Tests")
    inner class UpdateProfileTests {

        @Test
        @DisplayName("Should update user profile successfully")
        fun `should update profile successfully`() {
            // Given
            val userId = 1L
            val existingUser = User(
                id = userId,
                username = "testuser",
                email = "old@example.com",
                password = "hashedPassword",
                firstName = "Old",
                lastName = "Name",
                createdAt = LocalDateTime.now().minusDays(1),
                updatedAt = LocalDateTime.now().minusDays(1)
            )

            val updateRequest = UpdateProfileRequest(
                email = "new@example.com",
                firstName = "New",
                lastName = "Name"
            )

            val updatedUser = existingUser.copy(
                email = "new@example.com",
                firstName = "New",
                lastName = "Name",
                updatedAt = LocalDateTime.now()
            )

            val userRole = UserRoleEntity(
                id = 1L,
                userId = userId,
                role = UserRole.USER.name
            )

            whenever(userRepository.findById(userId))
                .thenReturn(Mono.just(existingUser))
            whenever(userRepository.existsByEmail("new@example.com"))
                .thenReturn(Mono.just(false))
            whenever(userRepository.save(any<User>()))
                .thenReturn(Mono.just(updatedUser))
            whenever(userRoleRepository.findByUserId(userId))
                .thenReturn(Flux.just(userRole))

            // When & Then
            StepVerifier.create(userService.updateProfile(userId, updateRequest))
                .expectNextMatches { response ->
                    response.email == "new@example.com" &&
                    response.firstName == "New" &&
                    response.lastName == "Name"
                }
                .verifyComplete()

            verify(userRepository).findById(any<Long>())
            verify(userRepository).save(any<User>())
        }

        @Test
        @DisplayName("Should throw ConflictException when email already exists")
        fun `should fail update when email already exists`() {
            // Given
            val userId = 1L
            val existingUser = User(
                id = userId,
                username = "testuser",
                email = "old@example.com",
                password = "hashedPassword",
                firstName = "Test",
                lastName = "User",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val updateRequest = UpdateProfileRequest(
                email = "taken@example.com",
                firstName = null,
                lastName = null
            )

            whenever(userRepository.findById(userId))
                .thenReturn(Mono.just(existingUser))
            whenever(userRepository.existsByEmail("taken@example.com"))
                .thenReturn(Mono.just(true))

            // When & Then
            StepVerifier.create(userService.updateProfile(userId, updateRequest))
                .expectErrorMatches { error ->
                    error is ConflictException &&
                    error.message == "Email already registered: taken@example.com"
                }
                .verify()

            verify(userRepository).findById(any<Long>())
            verify(userRepository).existsByEmail("taken@example.com")
            verify(userRepository, never()).save(any<User>())
        }

        @Test
        @DisplayName("Should update only provided fields")
        fun `should update only provided fields`() {
            // Given
            val userId = 1L
            val existingUser = User(
                id = userId,
                username = "testuser",
                email = "test@example.com",
                password = "hashedPassword",
                firstName = "Old",
                lastName = "Name",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val updateRequest = UpdateProfileRequest(
                email = null,
                firstName = "New",
                lastName = null
            )

            val updatedUser = existingUser.copy(
                firstName = "New",
                updatedAt = LocalDateTime.now()
            )

            val userRole = UserRoleEntity(
                id = 1L,
                userId = userId,
                role = UserRole.USER.name
            )

            whenever(userRepository.findById(userId))
                .thenReturn(Mono.just(existingUser))
            whenever(userRepository.save(any<User>()))
                .thenReturn(Mono.just(updatedUser))
            whenever(userRoleRepository.findByUserId(userId))
                .thenReturn(Flux.just(userRole))

            // When & Then
            StepVerifier.create(userService.updateProfile(userId, updateRequest))
                .expectNextMatches { response ->
                    response.firstName == "New" &&
                    response.lastName == "Name" &&
                    response.email == "test@example.com"
                }
                .verifyComplete()
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    inner class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        fun `should delete user successfully`() {
            // Given
            val userId = 1L
            val user = User(
                id = userId,
                username = "testuser",
                email = "test@example.com",
                password = "hashedPassword",
                firstName = "Test",
                lastName = "User",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            whenever(userRepository.findById(any<Long>()))
                .thenReturn(Mono.just(user))
            whenever(userRoleRepository.deleteByUserId(userId))
                .thenReturn(Mono.empty())
            whenever(userRepository.deleteById(any<Long>()))
                .thenReturn(Mono.empty())

            // When & Then
            StepVerifier.create(userService.deleteUser(userId))
                .verifyComplete()

            verify(userRepository).findById(any<Long>())
            verify(userRoleRepository).deleteByUserId(eq(userId))
            verify(userRepository).deleteById(any<Long>())
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        fun `should fail delete when user not found`() {
            // Given
            val userId = 999L

            whenever(userRepository.findById(any<Long>()))
                .thenReturn(Mono.empty())

            // When & Then
            StepVerifier.create(userService.deleteUser(userId))
                .expectErrorMatches { error ->
                    error is ResourceNotFoundException &&
                    error.message == "User not found with ID: $userId"
                }
                .verify()

            verify(userRepository).findById(any<Long>())
            verify(userRepository, never()).deleteById(any<Long>())
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    inner class RoleManagementTests {

        @Test
        @DisplayName("Should check if user has role successfully")
        fun `should check if user has role`() {
            // Given
            val userId = 1L
            val role = UserRole.MODERATOR

            whenever(userRoleRepository.existsByUserIdAndRole(userId, role.name))
                .thenReturn(Mono.just(true))

            // When & Then
            StepVerifier.create(userService.hasRole(userId, role))
                .expectNext(true)
                .verifyComplete()

            verify(userRoleRepository).existsByUserIdAndRole(userId, role.name)
        }

        @Test
        @DisplayName("Should add role to user successfully")
        fun `should add role to user successfully`() {
            // Given
            val userId = 1L
            val role = UserRole.SELLER
            val user = User(
                id = userId,
                username = "testuser",
                email = "test@example.com",
                password = "hashedPassword",
                firstName = "Test",
                lastName = "User",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val savedRole = UserRoleEntity(
                id = 2L,
                userId = userId,
                role = role.name
            )

            whenever(userRepository.findById(any<Long>()))
                .thenReturn(Mono.just(user))
            whenever(userRoleRepository.save(any<UserRoleEntity>()))
                .thenReturn(Mono.just(savedRole))

            // When & Then
            StepVerifier.create(userService.addRole(userId, role))
                .verifyComplete()

            verify(userRepository).findById(any<Long>())
            verify(userRoleRepository).save(any<UserRoleEntity>())
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when adding role to non-existent user")
        fun `should fail adding role when user not found`() {
            // Given
            val userId = 999L
            val role = UserRole.SELLER

            whenever(userRepository.findById(any<Long>()))
                .thenReturn(Mono.empty())

            // When & Then
            StepVerifier.create(userService.addRole(userId, role))
                .expectErrorMatches { error ->
                    error is ResourceNotFoundException &&
                    error.message == "User not found with ID: $userId"
                }
                .verify()

            verify(userRepository).findById(any<Long>())
            verify(userRoleRepository, never()).save(any<UserRoleEntity>())
        }
    }
}
