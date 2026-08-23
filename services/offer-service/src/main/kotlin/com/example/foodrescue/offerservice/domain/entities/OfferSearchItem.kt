package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.enum.Allergen
import com.example.foodrescue.offerservice.domain.enum.OfferStatus
import com.example.foodrescue.offerservice.domain.enum.FoodBagCategory
import java.time.ZoneId

class OfferSearchItem(
    val offerId: OfferId,
    val partnerId: PartnerId,
    val storeId: StoreId,
    val foodBagId: FoodBagId,
    val category: FoodBagCategory,
    val unitPrice: Money,
    allergens: Set<Allergen>,
    val status: OfferStatus,
    val availableQuantity: Int,
    val pickupWindow: PickupWindow,
    val storeName: String,
    val storeAddress: String,
    val storeTimeZone: ZoneId,
    val storeRating: Double?,
    val distanceMeters: Long?,
    val exactDistanceMeters: Double?,
) {
    private val allergenValues: Set<Allergen> = allergens.toSet()

    val allergens: Set<Allergen>
        get() = allergenValues.toSet()
}
