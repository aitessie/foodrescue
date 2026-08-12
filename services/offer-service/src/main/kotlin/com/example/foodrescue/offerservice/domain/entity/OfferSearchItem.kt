package com.example.foodrescue.offerservice.domain.entity

import com.example.foodrescue.offerservice.domain.enum.Allergen
import com.example.foodrescue.offerservice.domain.enum.OfferStatus
import com.example.foodrescue.offerservice.domain.enum.ProductCategory
import java.time.ZoneId

class OfferSearchItem(
    val offerId: OfferId,
    val partnerId: PartnerId,
    val storeId: StoreId,
    val productTemplateId: ProductTemplateId,
    val category: ProductCategory,
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
) {
    private val allergenValues: Set<Allergen> = allergens.toSet()

    val allergens: Set<Allergen>
        get() = allergenValues.toSet()
}
