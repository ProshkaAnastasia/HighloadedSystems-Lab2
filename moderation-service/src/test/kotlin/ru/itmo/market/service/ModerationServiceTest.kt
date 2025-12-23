package ru.itmo.market.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import ru.itmo.market.client.ProductServiceClient
import ru.itmo.market.client.UserServiceClient
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.model.entity.ModerationAction
import ru.itmo.market.model.entity.ModerationAudit
import ru.itmo.market.repository.ModerationActionRepository
import ru.itmo.market.repository.ModerationAuditRepository
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("Moderation Service Tests")
class ModerationServiceTest {

    @Mock
    private lateinit var productServiceClient: ProductServiceClient

    @Mock
    private lateinit var userServiceClient: UserServiceClient

    @Mock
    private lateinit var moderationActionRepository: ModerationActionRepository

    @Mock
    private lateinit var moderationAuditRepository: ModerationAuditRepository

    private lateinit var moderationService: ModerationService

    private val moderatorUser = UserResponse(
        id = 1L,
        username = "moderator",
        email = "mod@test.com",
        firstName = "Mod",
        lastName = "User",
        roles = setOf("MODERATOR"),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val adminUser = UserResponse(
        id = 2L,
        username = "admin",
        email = "admin@test.com",
        firstName = "Admin",
        lastName = "User",
        roles = setOf("ADMIN"),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val regularUser = UserResponse(
        id = 3L,
        username = "regular",
        email = "user@test.com",
        firstName = "Regular",
        lastName = "User",
        roles = setOf("USER"),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val testProduct = ProductResponse(
        id = 100L,
        name = "Test Product",
        description = "Test Description",
        price = BigDecimal("99.99"),
        imageUrl = null,
        shopId = 1L,
        sellerId = 10L,
        status = "PENDING",
        rejectionReason = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        moderationService = ModerationService(
            productServiceClient,
            userServiceClient,
            moderationActionRepository,
            moderationAuditRepository
        )
    }

    // ==================== getPendingProducts Tests ====================

    @Test
    @DisplayName("Should get pending products for moderator")
    fun testGetPendingProductsSuccess() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            totalElements = 1,
            totalPages = 1,
            page = 1,
            pageSize = 20
        )

        whenever(userServiceClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(productServiceClient.getPendingProducts(1, 20)).thenReturn(paginatedResponse)

        StepVerifier.create(moderationService.getPendingProducts(1L, 1, 20))
            .assertNext { response ->
                assert(response.data.size == 1)
                assert(response.data[0].id == 100L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should get pending products for admin")
    fun testGetPendingProductsForAdmin() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            totalElements = 1,
            totalPages = 1,
            page = 1,
            pageSize = 20
        )

        whenever(userServiceClient.getUserById(2L)).thenReturn(adminUser)
        whenever(productServiceClient.getPendingProducts(1, 20)).thenReturn(paginatedResponse)

        StepVerifier.create(moderationService.getPendingProducts(2L, 1, 20))
            .assertNext { response ->
                assert(response.data.size == 1)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting pending products for regular user")
    fun testGetPendingProductsForbidden() {
        whenever(userServiceClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.getPendingProducts(3L, 1, 20))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== getPendingProductById Tests ====================

    @Test
    @DisplayName("Should get pending product by ID for moderator")
    fun testGetPendingProductByIdSuccess() {
        whenever(userServiceClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(productServiceClient.getPendingProductById(100L)).thenReturn(testProduct)

        StepVerifier.create(moderationService.getPendingProductById(1L, 100L))
            .assertNext { product ->
                assert(product.id == 100L)
                assert(product.name == "Test Product")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting pending product for regular user")
    fun testGetPendingProductByIdForbidden() {
        whenever(userServiceClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.getPendingProductById(3L, 100L))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== approveProduct Tests ====================

    @Test
    @DisplayName("Should approve product successfully")
    fun testApproveProductSuccess() {
        val approvedProduct = testProduct.copy(status = "APPROVED")
        val savedAction = ModerationAction(
            id = 1L,
            productId = 100L,
            moderatorId = 1L,
            actionType = "APPROVE",
            reason = null
        )
        val savedAudit = ModerationAudit(
            id = 1L,
            actionId = 1L,
            productId = 100L,
            moderatorId = 1L,
            oldStatus = "PENDING",
            newStatus = "APPROVED"
        )

        whenever(userServiceClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(productServiceClient.approveProduct(100L, 1L)).thenReturn(approvedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.approveProduct(1L, 100L))
            .assertNext { result ->
                assert(result.productId == 100L)
                assert(result.action == "APPROVE")
                assert(result.newStatus == "APPROVED")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail approving product for regular user")
    fun testApproveProductForbidden() {
        whenever(userServiceClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.approveProduct(3L, 100L))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== rejectProduct Tests ====================

    @Test
    @DisplayName("Should reject product successfully")
    fun testRejectProductSuccess() {
        val rejectedProduct = testProduct.copy(status = "REJECTED", rejectionReason = "Low quality")
        val savedAction = ModerationAction(
            id = 1L,
            productId = 100L,
            moderatorId = 1L,
            actionType = "REJECT",
            reason = "Low quality"
        )
        val savedAudit = ModerationAudit(
            id = 1L,
            actionId = 1L,
            productId = 100L,
            moderatorId = 1L,
            oldStatus = "PENDING",
            newStatus = "REJECTED"
        )

        whenever(userServiceClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(productServiceClient.rejectProduct(100L, 1L, "Low quality")).thenReturn(rejectedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.rejectProduct(1L, 100L, "Low quality"))
            .assertNext { result ->
                assert(result.productId == 100L)
                assert(result.action == "REJECT")
                assert(result.reason == "Low quality")
                assert(result.newStatus == "REJECTED")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail rejecting product for regular user")
    fun testRejectProductForbidden() {
        whenever(userServiceClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.rejectProduct(3L, 100L, "Bad"))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== getModerationHistory Tests ====================

    @Test
    @DisplayName("Should get moderation history")
    fun testGetModerationHistory() {
        val actions = listOf(
            ModerationAction(id = 1L, productId = 100L, moderatorId = 1L, actionType = "APPROVE"),
            ModerationAction(id = 2L, productId = 101L, moderatorId = 1L, actionType = "REJECT", reason = "Bad")
        )

        whenever(moderationActionRepository.findByModeratorIdOrderByCreatedAtDesc(1L))
            .thenReturn(Flux.fromIterable(actions))

        StepVerifier.create(moderationService.getModerationHistory(1L))
            .assertNext { action ->
                assert(action.id == 1L)
                assert(action.actionType == "APPROVE")
            }
            .assertNext { action ->
                assert(action.id == 2L)
                assert(action.actionType == "REJECT")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should return empty history for new moderator")
    fun testGetModerationHistoryEmpty() {
        whenever(moderationActionRepository.findByModeratorIdOrderByCreatedAtDesc(999L))
            .thenReturn(Flux.empty())

        StepVerifier.create(moderationService.getModerationHistory(999L))
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    // ==================== getProductModerationHistory Tests ====================

    @Test
    @DisplayName("Should get product moderation history")
    fun testGetProductModerationHistory() {
        val audits = listOf(
            ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "APPROVED")
        )

        whenever(moderationAuditRepository.findByProductIdOrderByCreatedAtDesc(100L))
            .thenReturn(Flux.fromIterable(audits))

        StepVerifier.create(moderationService.getProductModerationHistory(100L))
            .assertNext { audit ->
                assert(audit.productId == 100L)
                assert(audit.newStatus == "APPROVED")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should return empty product history for new product")
    fun testGetProductModerationHistoryEmpty() {
        whenever(moderationAuditRepository.findByProductIdOrderByCreatedAtDesc(999L))
            .thenReturn(Flux.empty())

        StepVerifier.create(moderationService.getProductModerationHistory(999L))
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }
}
