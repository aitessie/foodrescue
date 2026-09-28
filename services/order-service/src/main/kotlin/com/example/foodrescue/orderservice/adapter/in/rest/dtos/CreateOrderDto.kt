package com.example.foodrescue.orderservice.adapter.`in`.rest.dtos

import jakarta.validation.constraints.Positive
import java.util.UUID

data class CreateOrderDto(
    val offerId: UUID,
    @field:Positive val quantity: Int,
)
