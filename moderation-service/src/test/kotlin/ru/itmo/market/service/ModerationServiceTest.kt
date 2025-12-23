package ru.itmo.market.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import ru.itmo.market.client.ProductServiceClient
import ru.itmo.market.client.UserServiceClient
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.model.entity.ModerationAction
import ru.itmo.market.model.entity.ModerationAudit
import ru.itmo.market.repository.ModerationActionRepository
import ru.itmo.market.repository.ModerationAuditRepository
import ru.itmo.market.exception.ForbiddenException
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
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

    private val moderatorId = 1L
    private val productId = 100L

    private val moderatorUser = UserResponse(
        id = moderatorId,
        username = "moderator",
        email = "moderator@test.com",
        firstName = "Test",
        lastName = "Moderator",
        roles = setOf("MODERATOR"),
        createdAt = LocalDateTime.now()
    )

    private val adminUser = UserResponse(
        id = 2L,
        username = "admin",
        email = "admin@test.com",
        firstName = "Test",
        lastName = "Admin",
        roles = setOf("ADMIN"),
        createdAt = LocalDateTime.now()
    )

    private val regularUser = UserResponse(
        id = 3L,
        username = "user",
        email = "user@test.com",
        firstName = "Test",
        lastName = "User",
        roles = setOf("USER"),
        createdAt = LocalDateTime.now()
    )

    private val testProduct = ProductResponse(
        id = productId,
        name = "Test Product",
        description = "Test Description",
        price = BigDecimal("99.99"),
        imageUrl = null,
        shopId = 1L,
        sellerId = 1L,
        status = "PENDING",
        rejectionReason = null,
        averageRating = null,
        commentsCount = null,
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

    // ============ GET PENDING PRODUCTS TESTS ============

    @Test
    fun `getPendingProducts should return products for moderator`() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            page = 0,
            pageSize = 10,
            totalElements = 1,
            totalPages = 1
        )

        whenever(userServiceClient.getUserById(moderatorId)).thenReturn(moderatorUser)
        whenever(productServiceClient.getPendingProducts(0, 10)).thenReturn(paginatedResponse)

        StepVerifier.create(moderationService.getPendingProducts(moderatorId, 0, 10))
            .expectNextMatches { response ->
                response.data.size == 1 &&
                response.data[0].name == "Test Product"
            }
            .verifyComplete()
    }

    @Test
    fun `getPendingProducts should return products for admin`() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            page = 0,
            pageSize = 10,
            totalElements = 1,
            totalPages = 1
        )

        whenever(userServiceClient.getUserById(2L)).thenReturn(adminUser)
        whenever(productServiceClient.getPendingProducts(0, 10)).thenReturn(paginatedResponse)

        StepVerifier.create(moderationService.getPendingProducts(2L, 0, 10))
            .expectNextMatches { response ->
                response.data.size == 1
            }
            .verifyComplete()
    }

    @Test
    fun `getPendingProducts should fail for regular user`() {
        whenever(userServiceClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.getPendingProducts(3L, 0, 10))
            .expectError(ForbiddenException::class.java)
            .verify()
    }

    // ============ GET PENDING PRODUCT BY ID TESTS ============

    @Test
    fun `getPendingProductById should return product for moderator`() {
        whenever(userServiceClient.getUserById(moderatorId)).thenReturn(moderatorUser)
        whenever(productServiceClient.getProductById(productId)).thenReturn(testProduct)

        StepVerifier.create(moderationService.getPendingProductById(moderatorId, productId))
            .expectNextMatches { product ->
                product.id == productId &&
                product.name == "Test Product"
            }
            .verifyComplete()
    }

    @Test
    fun `getPendingProductById should fail for non-moderator`() {
        whenever(userServiceClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.getPendingProductById(3L, productId))
            .expectError(RuntimeException::class.java)
            .verify()
    }

    // ============ APPROVE PRODUCT TESTS ============

    @Test
    fun `approveProduct should approve product and save action`() {
        val approvedProduct = testProduct.copy(status = "APPROVED")
        val savedAction = ModerationAction(
            id = 1L,
            productId = productId,
            moderatorId = moderatorId,
            actionType = "APPROVE",
            reason = null
        )
        val savedAudit = ModerationAudit(
            id = 1L,
            actionId = 1L,
            productId = productId,
            moderatorId = moderatorId,
            oldStatus = "PENDING",
            newStatus = "APPROVED"
        )

        whenever(userServiceClient.getUserById(moderatorId)).thenReturn(moderatorUser)
        whenever(productServiceClient.approveProduct(productId, moderatorId)).thenReturn(approvedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.approveProduct(moderatorId, productId))
            .expectNextMatches { result ->
                result.productId == productId &&
                result.action == "APPROVE" &&
                result.newStatus == "APPROVED" &&
                result.moderatorId == moderatorId
            }
            .verifyComplete()
    }

    @Test
    fun `approveProduct should work for admin`() {
        val approvedProduct = testProduct.copy(status = "APPROVED")
        val savedAction = ModerationAction(
            id = 1L,
            productId = productId,
            moderatorId = 2L,
            actionType = "APPROVE",
            reason = null
        )
        val savedAudit = ModerationAudit(
            id = 1L,
            actionId = 1L,
            productId = productId,
            moderatorId = 2L,
            oldStatus = "PENDING",
            newStatus = "APPROVED"
        )

        whenever(userServiceClient.getUserById(2L)).thenReturn(adminUser)
        whenever(productServiceClient.approveProduct(productId, 2L)).thenReturn(approvedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.approveProduct(2L, productId))
            .expectNextMatches { result ->
                result.action == "APPROVE" &&
                result.newStatus == "APPROVED"
            }
            .verifyComplete()
    }

    @Test
    fun `approveProduct should fail for regular user`() {
        whenever(userServiceClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.approveProduct(3L, productId))
            .expectError(RuntimeException::class.java)
            .verify()
    }

    // ============ REJECT PRODUCT TESTS ============

    @Test
    fun `rejectProduct should reject product with reason`() {
        val rejectedProduct = testProduct.copy(status = "REJECTED")
        val reason = "Inappropriate content"
        val savedAction = ModerationAction(
            id = 1L,
            productId = productId,
            moderatorId = moderatorId,
            actionType = "REJECT",
            reason = reason
        )
        val savedAudit = ModerationAudit(
            id = 1L,
            actionId = 1L,
            productId = productId,
            moderatorId = moderatorId,
            oldStatus = "PENDING",
            newStatus = "REJECTED"
        )

        whenever(userServiceClient.getUserById(moderatorId)).thenReturn(moderatorUser)
        whenever(productServiceClient.rejectProduct(productId, moderatorId, reason)).thenReturn(rejectedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.rejectProduct(moderatorId, productId, reason))
            .expectNextMatches { result ->
                result.productId == productId &&
                result.action == "REJECT" &&
                result.reason == reason &&
                result.newStatus == "REJECTED"
            }
            .verifyComplete()
    }

    @Test
    fun `rejectProduct should fail for regular user`() {
        whenever(userServiceClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.rejectProduct(3L, productId, "test reason"))
            .expectError(RuntimeException::class.java)
            .verify()
    }

    // ============ HISTORY TESTS ============

    @Test
    fun `getModerationHistory should return actions for moderator`() {
        val actions = listOf(
            ModerationAction(id = 1L, productId = 100L, moderatorId = moderatorId, actionType = "APPROVE", reason = null),
            ModerationAction(id = 2L, productId = 101L, moderatorId = moderatorId, actionType = "REJECT", reason = "Bad quality")
        )

        whenever(moderationActionRepository.findByModeratorIdOrderByCreatedAtDesc(moderatorId))
            .thenReturn(reactor.core.publisher.Flux.fromIterable(actions))

        StepVerifier.create(moderationService.getModerationHistory(moderatorId))
            .expectNextCount(2)
            .verifyComplete()
    }

    @Test
    fun `getProductModerationHistory should return audits for product`() {
        val audits = listOf(
            ModerationAudit(id = 1L, actionId = 1L, productId = productId, moderatorId = moderatorId, oldStatus = "PENDING", newStatus = "APPROVED")
        )

        whenever(moderationAuditRepository.findByProductIdOrderByCreatedAtDesc(productId))
            .thenReturn(reactor.core.publisher.Flux.fromIterable(audits))

        StepVerifier.create(moderationService.getProductModerationHistory(productId))
            .expectNextCount(1)
            .verifyComplete()
    }
}
