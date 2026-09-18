package com.example.foodrescue.orderservice.adapter.`in`.kafka.dtos

import java.util.UUID

data class OfferReservationRejectedEventPayloadDto(
    val reservationId: UUID,
    val offerId: UUID,
    val customerId: String,
    val quantity: Int,
    val reason: String,
)
