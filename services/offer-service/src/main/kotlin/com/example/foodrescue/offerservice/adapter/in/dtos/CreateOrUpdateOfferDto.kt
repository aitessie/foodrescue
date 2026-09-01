package com.example.foodrescue.offerservice.adapter.`in`.dtos

import com.example.foodrescue.offerservice.adapter.`in`.dtos.validation.ValidOfferPickupWindow
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.Instant
import java.util.UUID

@ValidOfferPickupWindow
data class CreateOrUpdateOfferDto(
    val foodBagId: UUID,
    @field:Positive val totalQuantity: Int,
    val pickupStart: Instant,
    val pickupEnd: Instant,
    @field:PositiveOrZero val version: Long,
)
