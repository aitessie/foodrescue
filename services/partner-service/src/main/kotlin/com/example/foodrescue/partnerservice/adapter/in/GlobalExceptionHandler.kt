package com.example.foodrescue.partnerservice.adapter.`in`

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.ApiExceptionResponse
import com.example.foodrescue.partnerservice.application.exception.EntityVersionConflictException
import com.example.foodrescue.partnerservice.application.exception.PartnerAccessDeniedException
import com.example.foodrescue.partnerservice.application.exception.PartnerNotFoundException
import com.example.foodrescue.partnerservice.application.exception.StoreAccessDeniedException
import com.example.foodrescue.partnerservice.application.exception.StoreNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntityVersionConflictException::class)
    fun handleVersionConflict(
        exception: EntityVersionConflictException
    ): ResponseEntity<ApiExceptionResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                ApiExceptionResponse(
                    code = "VERSION_CONFLICT",
                    message = exception.message ?: "Entity version conflict",
                )
            )
    }

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailure(
        exception: OptimisticLockingFailureException
    ): ResponseEntity<ApiExceptionResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                ApiExceptionResponse(
                    code = "CONCURRENT_UPDATE",
                    message = "The entity was concurrently updated",
                )
            )
    }

    @ExceptionHandler(
        PartnerNotFoundException::class,
        StoreNotFoundException::class,
    )
    fun handleNotFound(exception: RuntimeException): ResponseEntity<ApiExceptionResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ApiExceptionResponse(
                    code = "ENTITY_NOT_FOUND",
                    message = exception.message ?: "Entity was not found",
                )
            )
    }

    @ExceptionHandler(
        PartnerAccessDeniedException::class,
        StoreAccessDeniedException::class,
    )
    fun handleAccessDenied(exception: RuntimeException): ResponseEntity<ApiExceptionResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                ApiExceptionResponse(
                    code = "ACCESS_DENIED",
                    message = exception.message ?: "Access denied",
                )
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        exception: IllegalArgumentException
    ): ResponseEntity<ApiExceptionResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ApiExceptionResponse(
                    code = "INVALID_REQUEST",
                    message = exception.message ?: "Invalid request",
                )
            )
    }
}
