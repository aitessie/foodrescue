package com.example.foodrescue.offerservice.adapter.`in`.dtos

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

data class ChangeOfferQuantityDto(
    @field:Positive(message = "totalQuantity must be greater than zero") val totalQuantity: Int,
    @field:PositiveOrZero(message = "version must not be negative") val version: Long,
)
