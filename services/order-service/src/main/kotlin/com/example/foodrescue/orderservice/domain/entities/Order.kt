package com.example.foodrescue.orderservice.domain.entities

import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Instant

class Order(
    val id: OrderId,
    val customerId: String,
    val offerId: OfferId,
    val storeId: StoreId,
    val quantity: Int,
    val unitPrice: Long,
    val totalAmount: Long,
    val pickupStart: Instant,
    val pickupEnd: Instant,
    var status: OrderStatus,
    var version: Long,
    val createdAt: Instant,
    var updatedAt: Instant,
)
