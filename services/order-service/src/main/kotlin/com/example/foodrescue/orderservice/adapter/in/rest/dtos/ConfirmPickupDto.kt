package com.example.foodrescue.orderservice.adapter.`in`.rest.dtos

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class ConfirmPickupDto(
    val storeId: UUID,
    @field:NotBlank val token: String,
)
