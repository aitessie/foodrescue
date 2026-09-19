package com.example.foodrescue.orderservice.application.payments

import com.example.foodrescue.orderservice.domain.entities.OrderId

data class PaymentResult(
    val orderId: OrderId,
    val operation: PaymentOperation,
    val status: PaymentResultStatus,
    val amount: Long,
)
