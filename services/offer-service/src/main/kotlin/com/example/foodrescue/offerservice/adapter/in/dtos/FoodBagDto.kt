package com.example.foodrescue.offerservice.adapter.`in`.dtos

import com.example.foodrescue.offerservice.adapter.`in`.dtos.validation.ValidFoodBagPriceRange
import com.example.foodrescue.offerservice.domain.enum.Allergen
import com.example.foodrescue.offerservice.domain.enum.FoodBagCategory
import com.example.foodrescue.offerservice.domain.enum.MoneyCurrency
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

@ValidFoodBagPriceRange
data class FoodBagDto(
    @field:NotBlank(message = "name must not be blank")
    @field:Size(
        max = 255,
        message = "name must contain at most 255 characters",
    )
    val name: String,
    val description: String?,
    val category: FoodBagCategory,
    @field:Positive(message = "originalPriceMinor must be greater than zero")
    val originalPriceMinor: Long,
    @field:Positive(message = "unitPriceMinor must be greater than zero") val unitPriceMinor: Long,
    val currency: MoneyCurrency,
    val allergens: Set<Allergen>,
    @field:PositiveOrZero(message = "version must not be negative") val version: Long,
)
