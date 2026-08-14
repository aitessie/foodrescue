package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.enum.OfferSort
import java.time.Instant

data class OfferCursorData(
    val version: Int,
    val sort: OfferSort,
    val pickupEnd: Instant?,
    val unitPriceMinor: Long?,
    val distanceMeters: Double?,
    val offerId: OfferId,
    val filterFingerprint: String,
)
