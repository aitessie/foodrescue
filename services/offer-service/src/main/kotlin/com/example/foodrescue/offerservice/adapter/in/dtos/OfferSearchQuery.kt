package com.example.foodrescue.offerservice.adapter.`in`.dtos

import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import jakarta.validation.constraints.Min
import java.util.UUID

data class OfferSearchQuery(
    val storeId: UUID?,
    val category: FoodBagCategory?,
    @field:Min(
        value = 0,
        message = "page must not be negative",
    )
    val page: Int = 0,
    @field:Min(
        value = 1,
        message = "size must be greater than zero",
    )
    val size: Int = 20,
)
