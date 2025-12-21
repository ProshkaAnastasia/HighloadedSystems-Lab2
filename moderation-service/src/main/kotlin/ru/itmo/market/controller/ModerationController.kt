package ru.itmo.market.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.market.model.dto.request.BulkModerationRequest
import ru.itmo.market.model.dto.request.RejectProductRequest
import ru.itmo.market.model.dto.response.ModerationResultResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.service.ModerationService

@RestController
@RequestMapping("/api/moderation")
@Tag(name = "Moderation", description = "Модерация товаров API")
class ModerationController(
    private val moderationService: ModerationService
) {
    
    // ========== GET МЕТОДЫ ==========
    
    @GetMapping("/products")
    @Operation(
        summary = "Получить товары на модерации",
        description = "Получить список товаров со статусом PENDING для модератора"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Список товаров успешно получен"),
        ApiResponse(responseCode = "403", description = "Пользователь не модератор"),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    ])
    fun getPendingProducts(
        @RequestParam
        @Parameter(description = "ID модератора", example = "1")
        @Min(1, message = "moderatorId должен быть > 0")
        moderatorId: Long,
        
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы", example = "1")
        page: Int,
        
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Размер страницы", example = "20")
        pageSize: Int
    ): Mono<ResponseEntity<PaginatedResponse<ProductResponse>>> {
        return moderationService.getPendingProducts(moderatorId, page, pageSize)
            .map { ResponseEntity.ok(it) }
            .onErrorReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
    }
    
    @GetMapping("/products/{id}")
    @Operation(
        summary = "Получить товар на модерации по ID",
        description = "Получить подробную информацию о товаре, ожидающем модерации"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Товар найден"),
        ApiResponse(responseCode = "403", description = "Пользователь не модератор"),
        ApiResponse(responseCode = "404", description = "Товар не найден"),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    ])
    fun getPendingProductById(
        @RequestParam
        @Parameter(description = "ID модератора", example = "1")
        moderatorId: Long,
        
        @PathVariable
        @Parameter(description = "ID товара", example = "100")
        id: Long
    ): Mono<ResponseEntity<ProductResponse>> {
        return moderationService.getPendingProductById(moderatorId, id)
            .map { ResponseEntity.ok(it) }
            .onErrorReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
    }
    
    // ========== POST МЕТОДЫ ==========
    
    @PostMapping("/products/{id}/approve")
    @Operation(
        summary = "Одобрить товар",
        description = "Одобрить товар и изменить его статус на APPROVED"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Товар успешно одобрен"),
        ApiResponse(responseCode = "403", description = "Пользователь не модератор"),
        ApiResponse(responseCode = "404", description = "Товар не найден"),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    ])
    fun approveProduct(
        @RequestParam
        @Parameter(description = "ID модератора", example = "1")
        moderatorId: Long,
        
        @PathVariable
        @Parameter(description = "ID товара", example = "100")
        id: Long
    ): Mono<ResponseEntity<ModerationResultResponse>> {
        return moderationService.approveProduct(moderatorId, id)
            .map { ResponseEntity.status(HttpStatus.CREATED).body(it) }
            .onErrorReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
    }
    
    @PostMapping("/products/{id}/reject")
    @Operation(
        summary = "Отклонить товар",
        description = "Отклонить товар и изменить его статус на REJECTED с указанием причины"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Товар успешно отклонен"),
        ApiResponse(responseCode = "400", description = "Некорректный запрос"),
        ApiResponse(responseCode = "403", description = "Пользователь не модератор"),
        ApiResponse(responseCode = "404", description = "Товар не найден"),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    ])
    fun rejectProduct(
        @RequestParam
        @Parameter(description = "ID модератора", example = "1")
        moderatorId: Long,
        
        @PathVariable
        @Parameter(description = "ID товара", example = "100")
        id: Long,
        
        @Valid
        @RequestBody
        request: RejectProductRequest
    ): Mono<ResponseEntity<ModerationResultResponse>> {
        return moderationService.rejectProduct(moderatorId, id, request.reason)
            .map { ResponseEntity.status(HttpStatus.CREATED).body(it) }
            .onErrorReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
    }
    
    @PostMapping("/bulk")
    @Operation(
        summary = "Массовая модерация товаров",
        description = "Одобрить или отклонить несколько товаров за раз"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Товары успешно обработаны"),
        ApiResponse(responseCode = "400", description = "Некорректный запрос"),
        ApiResponse(responseCode = "403", description = "Пользователь не модератор"),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    ])
    fun bulkModerate(
        @RequestParam
        @Parameter(description = "ID модератора", example = "1")
        moderatorId: Long,
        
        @Valid
        @RequestBody
        request: BulkModerationRequest
    ): Flux<ModerationResultResponse> {
        return moderationService.bulkModerate(moderatorId, request)
    }
    
    // ========== ИСТОРИЯ ==========
    
    @GetMapping("/history")
    @Operation(
        summary = "История модерации модератора",
        description = "Получить историю всех действий модерации конкретного модератора"
    )
    fun getModerationHistory(
        @RequestParam
        @Parameter(description = "ID модератора", example = "1")
        moderatorId: Long
    ): Flux<ru.itmo.market.model.entity.ModerationAction> {
        return moderationService.getModerationHistory(moderatorId)
    }
    
    @GetMapping("/products/{id}/history")
    @Operation(
        summary = "История модерации товара",
        description = "Получить историю всех действий модерации для конкретного товара"
    )
    fun getProductModerationHistory(
        @PathVariable
        @Parameter(description = "ID товара", example = "100")
        id: Long
    ): Flux<ru.itmo.market.model.entity.ModerationAudit> {
        return moderationService.getProductModerationHistory(id)
    }
}
