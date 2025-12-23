package ru.itmo.userservice.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.MethodArgumentNotValidException
import ru.itmo.userservice.model.dto.response.ErrorResponse
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                message = ex.message ?: "Resource not found",
                status = HttpStatus.NOT_FOUND.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(ex: ConflictException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(
                message = ex.message ?: "Resource conflict",
                status = HttpStatus.CONFLICT.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(ex: BadRequestException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                message = ex.message ?: "Bad request",
                status = HttpStatus.BAD_REQUEST.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                message = "Validation failed",
                status = HttpStatus.BAD_REQUEST.value(),
                timestamp = LocalDateTime.now(),
                path = "",
                errors = errors
            ))
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                message = "Internal server error: ${ex.message}",
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }
}
