package com.example.foodrescue.offerservice.adapter.`in`.dtos

import jakarta.validation.constraints.Positive

data class ReserveFoodBagsDto(
    @field:Positive(message = "quantity must be greater than zero") val quantity: Int
)
