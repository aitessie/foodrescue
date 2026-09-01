package com.example.foodrescue.offerservice.adapter.`in`.dtos

import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import jakarta.validation.constraints.PositiveOrZero

data class FoodBagStatusDto(
    val status: FoodBagStatus,
    @field:PositiveOrZero(message = "version must not be negative") val version: Long,
)
