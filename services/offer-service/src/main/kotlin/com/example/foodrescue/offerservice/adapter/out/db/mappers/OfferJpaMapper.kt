package com.example.foodrescue.offerservice.adapter.`out`.db.mappers

import com.example.foodrescue.offerservice.adapter.`out`.db.entities.OfferJpaEntity
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import org.springframework.stereotype.Component

@Component
class OfferJpaMapper {
    fun toDomain(entity: OfferJpaEntity): Offer =
        Offer(
            id = OfferId(entity.id),
            storeId = StoreId(entity.storeId),
            foodBagId = FoodBagId(entity.foodBagId),
            category = entity.category,
            unitPrice =
                Money(
                    amountMinor = entity.unitPriceMinor,
                    currency = entity.currency,
                ),
            allergens = entity.allergens.toSet(),
            status = entity.status,
            totalQuantity = entity.totalQuantity,
            availableQuantity = entity.availableQuantity,
            pickupWindow =
                PickupWindow(
                    start = entity.pickupStart,
                    end = entity.pickupEnd,
                ),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            version = entity.version,
        )

    fun toJpaEntity(offer: Offer): OfferJpaEntity =
        OfferJpaEntity(
            id = offer.id.value,
            storeId = offer.storeId.value,
            foodBagId = offer.foodBagId.value,
            category = offer.category,
            unitPriceMinor = offer.unitPrice.amountMinor,
            currency = offer.unitPrice.currency,
            allergens = offer.allergens.toMutableSet(),
            status = offer.status,
            totalQuantity = offer.totalQuantity,
            availableQuantity = offer.availableQuantity,
            pickupStart = offer.pickupWindow.start,
            pickupEnd = offer.pickupWindow.end,
            createdAt = offer.createdAt,
            updatedAt = offer.updatedAt,
            version = offer.version,
        )
}
