package com.example.foodrescue.orderservice.domain.entities

import com.example.foodrescue.orderservice.domain.`enum`.OfferStatus
import java.time.Instant

data class OfferSnapshot(
    val offerId: OfferId,
    val storeId: StoreId,
    val status: OfferStatus,
    val unitPrice: Long,
    val availableQuantity: Int,
    val pickupStart: Instant,
    val pickupEnd: Instant,
)
