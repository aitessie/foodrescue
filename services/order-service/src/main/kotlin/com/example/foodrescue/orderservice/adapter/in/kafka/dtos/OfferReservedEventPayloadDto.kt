package com.example.foodrescue.orderservice.adapter.`in`.kafka.dtos

import java.time.Instant
import java.util.UUID

data class OfferReservedEventPayloadDto(
    val reservationId: UUID,
    val offerId: UUID,
    val quantity: Int,
    val reservationStatus: String,
    val offerTotalQuantity: Int,
    val offerAvailableQuantity: Int,
    val offerReservedQuantity: Int,
    val reservationCreatedAt: Instant,
    val reservationUpdatedAt: Instant,
)
