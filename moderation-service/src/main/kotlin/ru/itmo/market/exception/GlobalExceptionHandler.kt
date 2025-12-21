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
    fun handleForbiddenException(ex: ForbiddenException, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorResponse>> {
        val error = ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            message = ex.message ?: "Forbidden",
            timestamp = LocalDateTime.now()
        )
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(error))
    }
    
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorResponse>> {
        val error = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            message = ex.message ?: "Resource not found",
            timestamp = LocalDateTime.now()
        )
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error))
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorResponse>> {
        val error = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = ex.message ?: "Bad request",
            timestamp = LocalDateTime.now()
        )
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error))
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorResponse>> {
        val error = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = "Internal server error",
            timestamp = LocalDateTime.now()
        )
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error))
    }
}
