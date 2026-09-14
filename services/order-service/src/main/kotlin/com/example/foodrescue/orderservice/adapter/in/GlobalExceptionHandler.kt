package com.example.foodrescue.orderservice.adapter.`in`

import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFound(exception: OrderNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.message ?: "Order not found",
        )
}
