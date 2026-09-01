package com.example.foodrescue.offerservice.application.events

import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.Allergen
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import java.time.Instant

class OfferEventPayload(
    val offerId: OfferId,
    val storeId: StoreId,
    val foodBagId: FoodBagId,
    val category: FoodBagCategory,
    val unitPrice: Money,
    allergens: Set<Allergen>,
    val status: OfferStatus,
    val totalQuantity: Int,
    val availableQuantity: Int,
    val pickupWindow: PickupWindow,
    val createdAt: Instant,
    val updatedAt: Instant,
) : ApplicationEventPayload {
    private val allergenValues: Set<Allergen> = allergens.toSet()

    val allergens: Set<Allergen>
        get() = allergenValues.toSet()
}
