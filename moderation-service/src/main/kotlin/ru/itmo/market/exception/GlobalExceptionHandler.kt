package ru.itmo.market.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import ru.itmo.market.model.dto.response.ErrorResponse

@RestControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(
        ex: ForbiddenException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse(
                    message = ex.message ?: "Forbidden",
                    status = HttpStatus.FORBIDDEN.value(),
                    timestamp = LocalDateTime.now(),
                    path = exchange.request.path.value()
                ))
        )
    }
    
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(
        ex: ResourceNotFoundException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(
                    message = ex.message ?: "Resource not found",
                    status = HttpStatus.NOT_FOUND.value(),
                    timestamp = LocalDateTime.now(),
                    path = exchange.request.path.value()
                ))
        )
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(
                    message = ex.message ?: "Bad request",
                    status = HttpStatus.BAD_REQUEST.value(),
                    timestamp = LocalDateTime.now(),
                    path = exchange.request.path.value()
                ))
        )
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(
                    message = ex.message ?: "Internal server error",
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    timestamp = LocalDateTime.now(),
                    path = exchange.request.path.value()
                ))
        )
    }
}
