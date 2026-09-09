package com.example.foodrescue.offerservice.adapter.`out`.db.mappers

import com.example.foodrescue.offerservice.adapter.`out`.db.entities.FoodBagJpaEntity
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import org.springframework.stereotype.Component

@Component
class FoodBagJpaMapper {
    fun toDomain(entity: FoodBagJpaEntity): FoodBag =
        FoodBag(
            id = FoodBagId(entity.id),
            storeId = StoreId(entity.storeId),
            name = entity.name,
            description = entity.description,
            category = entity.category,
            originalPrice = entity.originalPrice,
            unitPrice = entity.unitPrice,
            allergens = entity.allergens.toSet(),
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            version = entity.version,
        )

    fun toJpaEntity(foodBag: FoodBag): FoodBagJpaEntity =
        FoodBagJpaEntity(
            id = foodBag.id.value,
            storeId = foodBag.storeId.value,
            name = foodBag.name,
            description = foodBag.description,
            category = foodBag.category,
            originalPrice = foodBag.originalPrice,
            unitPrice = foodBag.unitPrice,
            allergens = foodBag.allergens.toMutableSet(),
            status = foodBag.status,
            createdAt = foodBag.createdAt,
            updatedAt = foodBag.updatedAt,
            version = foodBag.version,
        )
}
