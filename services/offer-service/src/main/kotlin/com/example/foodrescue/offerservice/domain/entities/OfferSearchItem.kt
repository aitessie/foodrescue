package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.`enum`.Allergen
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import java.time.ZoneId

class OfferSearchItem(
    val offerId: OfferId,
    val storeId: StoreId,
    val foodBagId: FoodBagId,
    val foodBagName: String,
    val foodBagDescription: String?,
    val category: FoodBagCategory,
    val originalPrice: Money,
    val unitPrice: Money,
    allergens: Set<Allergen>,
    val availableQuantity: Int,
    val pickupWindow: PickupWindow,
    val storeName: String,
    val storeAddress: String,
    val storeTimeZone: ZoneId,
) {
    private val allergenValues: Set<Allergen> = allergens.toSet()

    val allergens: Set<Allergen>
        get() = allergenValues.toSet()
}
