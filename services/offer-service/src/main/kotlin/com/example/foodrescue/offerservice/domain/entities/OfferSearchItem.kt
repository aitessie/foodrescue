package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.`enum`.Allergen
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.enum.OfferStatus

class OfferSearchItem(
    val offerId: OfferId,
    val storeId: StoreId,
    val foodBagId: FoodBagId,
    val foodBagName: String,
    val foodBagDescription: String?,
    val category: FoodBagCategory,
    val status: OfferStatus,
    val originalPrice: Long,
    val unitPrice: Long,
    allergens: Set<Allergen>,
    val availableQuantity: Int,
    val pickupWindow: PickupWindow,
    val storeName: String,
    val storeAddress: String,
) {
    private val allergenValues: Set<Allergen> = allergens.toSet()

    val allergens: Set<Allergen>
        get() = allergenValues.toSet()
}
