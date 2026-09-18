package com.example.foodrescue.orderservice.adapter.`in`

import com.example.foodrescue.orderservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.orderservice.application.exceptions.OfferServiceAuthenticationException
import com.example.foodrescue.orderservice.application.exceptions.OfferServiceContractException
import com.example.foodrescue.orderservice.application.exceptions.OfferServiceUnavailableException
import com.example.foodrescue.orderservice.application.exceptions.OrderAccessDeniedException
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.exceptions.OrderValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(OrderValidationException::class)
    fun handleOrderValidation(exception: OrderValidationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            exception.message ?: "Order validation failed",
        )

    @ExceptionHandler(OrderAccessDeniedException::class)
    fun handleOrderAccessDenied(exception: OrderAccessDeniedException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.message ?: "Access denied")

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFound(exception: OrderNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.message ?: "Order not found",
        )

    @ExceptionHandler(OfferNotFoundException::class)
    fun handleOfferNotFound(exception: OfferNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.message ?: "Offer not found",
        )

    @ExceptionHandler(OrderConflictException::class)
    fun handleOrderConflict(exception: OrderConflictException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            exception.message ?: "Order conflict",
        )

    @ExceptionHandler(OfferServiceUnavailableException::class)
    fun handleOfferServiceUnavailable(exception: OfferServiceUnavailableException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.message ?: "Offer Service is unavailable",
        )

    @ExceptionHandler(OfferServiceAuthenticationException::class)
    fun handleOfferServiceAuthentication(
        exception: OfferServiceAuthenticationException
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.message ?: "Offer Service authentication is unavailable",
        )

    @ExceptionHandler(OfferServiceContractException::class)
    fun handleOfferServiceContract(exception: OfferServiceContractException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.message ?: "Offer Service returned an invalid response",
        )
}
