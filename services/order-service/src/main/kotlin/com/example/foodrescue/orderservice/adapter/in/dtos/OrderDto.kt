package com.example.foodrescue.orderservice.adapter.`in`.dtos

import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Instant
import java.util.UUID

class OrderDto(
    val orderId: UUID,
    val offerId: UUID,
    val storeId: UUID,
    val quantity: Int,
    val unitPrice: Long,
    val totalAmount: Long,
    val pickupStart: Instant,
    val pickupEnd: Instant,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)
