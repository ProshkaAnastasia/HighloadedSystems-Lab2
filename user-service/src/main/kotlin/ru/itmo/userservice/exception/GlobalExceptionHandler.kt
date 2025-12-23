package ru.itmo.userservice.exception

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.bind.MethodArgumentNotValidException
import ru.itmo.userservice.model.dto.response.ErrorResponse
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    
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
    
    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(
        ex: ConflictException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse(
                    message = ex.message ?: "Resource conflict",
                    status = HttpStatus.CONFLICT.value(),
                    timestamp = LocalDateTime.now(),
                    path = exchange.request.path.value()
                ))
        )
    }
    
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(
        ex: BadRequestException,
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
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errors = ex.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }
        
        return Mono.just(
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(
                    message = "Validation failed",
                    status = HttpStatus.BAD_REQUEST.value(),
                    timestamp = LocalDateTime.now(),
                    path = exchange.request.path.value(),
                    errors = errors
                ))
        )
    } 
    
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(
                    message = "Internal server error: ${ex.message}",
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    timestamp = LocalDateTime.now(),
                    path = exchange.request.path.value()
                ))
        )
    }
}
