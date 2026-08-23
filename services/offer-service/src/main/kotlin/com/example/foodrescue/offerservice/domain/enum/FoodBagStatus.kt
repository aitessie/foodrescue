package com.example.foodrescue.offerservice.domain.enum

enum class FoodBagStatus(
    val code: String,
    val description: String,
) {
    ACTIVE("ACTIVE", "Активен"),
    INACTIVE("INACTIVE", "Неактивен"),
}
