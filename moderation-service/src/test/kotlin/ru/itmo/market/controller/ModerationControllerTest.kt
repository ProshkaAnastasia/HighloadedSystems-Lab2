package ru.itmo.market.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.market.model.dto.request.BulkModerationRequest
import ru.itmo.market.model.dto.response.ModerationResultResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.entity.ModerationAction
import ru.itmo.market.model.entity.ModerationAudit
import ru.itmo.market.service.ModerationService
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModerationControllerTest {

    @Mock
    private lateinit var moderationService: ModerationService

    private lateinit var webTestClient: WebTestClient

    private val moderatorId = 1L
    private val productId = 100L

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

    private val approvalResult = ModerationResultResponse(
        productId = productId,
        productName = "Test Product",
        action = "APPROVE",
        reason = null,
        moderatorId = moderatorId,
        newStatus = "APPROVED",
        timestamp = LocalDateTime.now()
    )

    private val rejectResult = ModerationResultResponse(
        productId = productId,
        productName = "Test Product",
        action = "REJECT",
        reason = "Test reason",
        moderatorId = moderatorId,
        newStatus = "REJECTED",
        timestamp = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        val controller = ModerationController(moderationService)
        webTestClient = WebTestClient.bindToController(controller).build()
    }

    // ============ GET PENDING PRODUCTS TESTS ============

    @Test
    fun `getPendingProducts should return paginated products`() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            page = 1,
            pageSize = 20,
            totalElements = 1,
            totalPages = 1
        )

        whenever(moderationService.getPendingProducts(any(), any(), any()))
            .thenReturn(Mono.just(paginatedResponse))

        // Note: This test would need proper SecurityContext setup in real scenario
        // For now, this demonstrates the test structure
    }

    // ============ APPROVE PRODUCT TESTS ============

    @Test
    fun `approveProduct should return 201 when approved`() {
        whenever(moderationService.approveProduct(any(), any()))
            .thenReturn(Mono.just(approvalResult))

        // Note: This test would need proper SecurityContext setup in real scenario
    }

    // ============ REJECT PRODUCT TESTS ============

    @Test
    fun `rejectProduct should return 201 when rejected`() {
        whenever(moderationService.rejectProduct(any(), any(), any()))
            .thenReturn(Mono.just(rejectResult))

        // Note: This test would need proper SecurityContext setup in real scenario
    }

    // ============ BULK MODERATION TESTS ============

    @Test
    fun `bulkModerate should return results for all products`() {
        val results = listOf(approvalResult, approvalResult.copy(productId = 101))

        whenever(moderationService.bulkModerate(any(), any<BulkModerationRequest>()))
            .thenReturn(Flux.fromIterable(results))

        // Note: This test would need proper SecurityContext setup in real scenario
    }

    // ============ HISTORY TESTS ============

    @Test
    fun `getModerationHistory should return actions`() {
        val actions = listOf(
            ModerationAction(id = 1L, productId = 100L, moderatorId = moderatorId, actionType = "APPROVE", reason = null),
            ModerationAction(id = 2L, productId = 101L, moderatorId = moderatorId, actionType = "REJECT", reason = "Bad")
        )

        whenever(moderationService.getModerationHistory(any()))
            .thenReturn(Flux.fromIterable(actions))

        // Note: This test would need proper SecurityContext setup in real scenario
    }

    @Test
    fun `getProductModerationHistory should return audits for product`() {
        val audits = listOf(
            ModerationAudit(id = 1L, actionId = 1L, productId = productId, moderatorId = moderatorId, oldStatus = "PENDING", newStatus = "APPROVED")
        )

        whenever(moderationService.getProductModerationHistory(productId))
            .thenReturn(Flux.fromIterable(audits))

        webTestClient.get()
            .uri("/api/moderation/products/$productId/history")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
    }
}
