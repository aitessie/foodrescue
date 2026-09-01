package com.example.foodrescue.offerservice.adapter.`in`.dtos

import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import jakarta.validation.constraints.PositiveOrZero

data class OfferStatusDto(
    val status: OfferStatus,
    @field:PositiveOrZero(message = "version must not be negative") val version: Long,
)
