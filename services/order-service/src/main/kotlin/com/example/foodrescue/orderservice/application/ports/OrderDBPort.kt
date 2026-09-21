package com.example.foodrescue.orderservice.application.ports

import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.OrderPage
import java.time.Instant

interface OrderDBPort {
    fun findById(orderId: OrderId): Order?

    fun findByCustomerId(
        customerId: String,
        page: Int,
        size: Int,
    ): OrderPage

    fun findNoShowCandidates(
        pickupEndedAt: Instant,
        batchSize: Int,
    ): List<Order>

    fun save(order: Order): Order
}
