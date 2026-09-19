package com.example.foodrescue.orderservice.application.ports

import com.example.foodrescue.orderservice.domain.entities.OrderId

interface PaymentCommandPort {
    fun requestAuthorization(
        orderId: OrderId,
        amount: Long,
    )

    fun requestCapture(
        orderId: OrderId,
        amount: Long,
    )

    fun requestVoid(
        orderId: OrderId,
        amount: Long,
    )

    fun requestRefund(
        orderId: OrderId,
        amount: Long,
    )
}
