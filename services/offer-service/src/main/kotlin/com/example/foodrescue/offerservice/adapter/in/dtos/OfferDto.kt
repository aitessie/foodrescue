package com.example.foodrescue.offerservice.adapter.`in`.dtos

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.Instant
import java.util.*

data class OfferDto(
    val foodBagId: UUID,
    @field:Positive(message = "totalQuantity must be greater than zero") val totalQuantity: Int,
    val availableQuantity: Int?,
    val pickupStart: Instant,
    val pickupEnd: Instant,
    @field:PositiveOrZero(message = "version must not be negative") val version: Long,
)
