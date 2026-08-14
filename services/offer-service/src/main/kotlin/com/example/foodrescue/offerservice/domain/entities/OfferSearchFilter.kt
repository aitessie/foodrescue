package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.enum.Allergen
import com.example.foodrescue.offerservice.domain.enum.DateFilter
import com.example.foodrescue.offerservice.domain.enum.OfferSort
import com.example.foodrescue.offerservice.domain.enum.ProductCategory
import java.time.Instant
import java.time.ZoneId

class OfferSearchFilter(
    val date: DateFilter?,
    val timeZone: ZoneId?,
    val category: ProductCategory?,
    val minPriceMinor: Long?,
    val maxPriceMinor: Long?,
    val pickupStart: Instant?,
    val pickupEnd: Instant?,
    excludedAllergens: Set<Allergen>,
    val minRating: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val radiusMeters: Int?,
    val sort: OfferSort,
    val cursor: String?,
    val limit: Int,
) {
    private val excludedAllergenValues: Set<Allergen> = excludedAllergens.toSet()

    val excludedAllergens: Set<Allergen>
        get() = excludedAllergenValues.toSet()
}
