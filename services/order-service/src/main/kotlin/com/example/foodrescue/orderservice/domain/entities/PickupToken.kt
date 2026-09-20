package com.example.foodrescue.orderservice.domain.entities

import java.time.Instant

class PickupToken(
    val orderId: OrderId,
    val tokenHash: String,
    val usedAt: Instant?,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
