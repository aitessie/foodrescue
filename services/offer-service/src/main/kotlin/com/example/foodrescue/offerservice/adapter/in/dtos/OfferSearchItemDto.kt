package com.example.foodrescue.offerservice.adapter.`in`.dtos

import com.example.foodrescue.offerservice.domain.`enum`.Allergen
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class OfferSearchItemDto(
    val offerId: UUID,
    val storeId: UUID,
    val foodBagId: UUID,
    val foodBagName: String,
    val foodBagDescription: String?,
    val category: FoodBagCategory,
    val originalPriceMinor: Long,
    val unitPriceMinor: Long,
    val currency: MoneyCurrency,
    val allergens: Set<Allergen>,
    val availableQuantity: Int,
    val pickupStart: Instant,
    val pickupEnd: Instant,
    val storeName: String,
    val storeAddress: String,
    val storeTimeZone: ZoneId,
)
