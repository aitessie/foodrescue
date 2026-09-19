package com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos

import java.util.UUID

data class OrderReservationEventPayloadDto(
    val orderId: UUID,
    val offerId: UUID,
    val customerId: String,
    val quantity: Int,
)
