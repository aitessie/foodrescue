package com.example.foodrescue.offerservice.adapter.out.db.mappers

import com.example.foodrescue.offerservice.adapter.out.db.persistence.PublicOfferJpaProjection
import com.example.foodrescue.offerservice.domain.entities.OfferSearchItem
import org.springframework.stereotype.Component

@Component
class PublicOfferJpaMapper(
    private val offerJpaMapper: OfferJpaMapper,
    private val foodBagJpaMapper: FoodBagJpaMapper,
    private val storeSnapshotJpaMapper: StoreSnapshotJpaMapper,
) {
    fun toDomain(projection: PublicOfferJpaProjection): OfferSearchItem {
        val offer = offerJpaMapper.toDomain(projection.offer)
        val foodBag = foodBagJpaMapper.toDomain(projection.foodBag)
        val storeSnapshot = storeSnapshotJpaMapper.toDomain(projection.storeSnapshot)

        return OfferSearchItem(
            offerId = offer.id,
            storeId = offer.storeId,
            foodBagId = offer.foodBagId,
            foodBagName = foodBag.name,
            foodBagDescription = foodBag.description,
            category = offer.category,
            originalPrice = foodBag.originalPrice,
            unitPrice = offer.unitPrice,
            allergens = offer.allergens,
            availableQuantity = offer.availableQuantity,
            pickupWindow = offer.pickupWindow,
            storeName = storeSnapshot.name,
            storeAddress = storeSnapshot.address,
            storeTimeZone = storeSnapshot.timeZone,
        )
    }
}
