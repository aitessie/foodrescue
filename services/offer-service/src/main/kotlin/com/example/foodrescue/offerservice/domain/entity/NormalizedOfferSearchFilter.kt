package com.example.foodrescue.offerservice.domain.entity

import com.example.foodrescue.offerservice.domain.enum.Allergen
import com.example.foodrescue.offerservice.domain.enum.OfferSort
import com.example.foodrescue.offerservice.domain.enum.ProductCategory
import java.time.Instant

class NormalizedOfferSearchFilter(
    val category: ProductCategory?,
    val minPriceMinor: Long?,
    val maxPriceMinor: Long?,
    val pickupStartInclusive: Instant?,
    val pickupEndExclusive: Instant?,
    excludedAllergens: Set<Allergen>,
    val minRating: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val radiusMeters: Int?,
    val sort: OfferSort,
) {
    private val excludedAllergenValues: Set<Allergen> = excludedAllergens.toSet()

    val excludedAllergens: Set<Allergen>
        get() = excludedAllergenValues.toSet()

    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null

    val hasRadius: Boolean
        get() = radiusMeters != null
}
