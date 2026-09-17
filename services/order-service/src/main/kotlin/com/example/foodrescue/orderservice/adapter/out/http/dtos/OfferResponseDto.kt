package com.example.foodrescue.orderservice.adapter.out.http.dtos

import com.example.foodrescue.orderservice.domain.enum.OfferStatus
import java.time.Instant
import java.util.UUID

data class OfferResponseDto(
    val offerId: UUID,
    val storeId: UUID,
    val unitPrice: Long,
    val availableQuantity: Int,
    val pickupStart: Instant,
    val pickupEnd: Instant,
    val status: OfferStatus,
)
