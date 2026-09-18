package com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos

import java.util.UUID

data class OrderReservationRequestedPayloadDto(
    val orderId: UUID,
    val offerId: UUID,
    val customerId: String,
    val quantity: Int,
)
