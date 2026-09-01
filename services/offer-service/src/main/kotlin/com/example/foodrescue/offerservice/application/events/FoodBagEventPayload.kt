package com.example.foodrescue.offerservice.application.events

import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.Allergen
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import java.time.Instant

class FoodBagEventPayload(
    val foodBagId: FoodBagId,
    val storeId: StoreId,
    val name: String,
    val description: String?,
    val category: FoodBagCategory,
    val originalPrice: Money,
    val unitPrice: Money,
    allergens: Set<Allergen>,
    val status: FoodBagStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) : ApplicationEventPayload {
    private val allergenValues: Set<Allergen> = allergens.toSet()

    val allergens: Set<Allergen>
        get() = allergenValues.toSet()
}
