package com.example.foodrescue.orderservice.adapter.`in`

import com.example.foodrescue.orderservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.orderservice.application.exceptions.OfferServiceAuthenticationException
import com.example.foodrescue.orderservice.application.exceptions.OfferServiceContractException
import com.example.foodrescue.orderservice.application.exceptions.OfferServiceUnavailableException
import com.example.foodrescue.orderservice.application.exceptions.OrderAccessDeniedException
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.exceptions.OrderValidationException
import com.example.foodrescue.orderservice.application.exceptions.PartnerServiceAuthenticationException
import com.example.foodrescue.orderservice.application.exceptions.PartnerServiceContractException
import com.example.foodrescue.orderservice.application.exceptions.PartnerServiceUnavailableException
import com.example.foodrescue.orderservice.application.exceptions.PartnerStoreNotFoundException
import com.example.foodrescue.orderservice.application.exceptions.PickupAccessDeniedException
import com.example.foodrescue.orderservice.application.exceptions.PickupTokenNotFoundException
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

    @ExceptionHandler(PickupAccessDeniedException::class)
    fun handlePickupAccessDenied(exception: PickupAccessDeniedException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.message ?: "Access denied")

    @ExceptionHandler(PickupTokenNotFoundException::class)
    fun handlePickupTokenNotFound(exception: PickupTokenNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.message ?: "Pickup token not found",
        )

    @ExceptionHandler(PartnerStoreNotFoundException::class)
    fun handlePartnerStoreNotFound(exception: PartnerStoreNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.message ?: "Store not found",
        )

    @ExceptionHandler(PartnerServiceUnavailableException::class)
    fun handlePartnerServiceUnavailable(
        exception: PartnerServiceUnavailableException
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.message ?: "Partner Service is unavailable",
        )

    @ExceptionHandler(PartnerServiceAuthenticationException::class)
    fun handlePartnerServiceAuthentication(
        exception: PartnerServiceAuthenticationException
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.message ?: "Partner Service authentication is unavailable",
        )

    @ExceptionHandler(PartnerServiceContractException::class)
    fun handlePartnerServiceContract(exception: PartnerServiceContractException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.message ?: "Partner Service returned an invalid response",
        )
}
