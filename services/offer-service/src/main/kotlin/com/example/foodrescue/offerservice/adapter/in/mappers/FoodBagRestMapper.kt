package com.example.foodrescue.offerservice.adapter.`in`.mappers

import com.example.foodrescue.offerservice.adapter.`in`.dtos.FoodBagDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.FoodBagStatusDto
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class FoodBagRestMapper(private val clock: Clock) {
    fun toDomain(
        storeId: UUID,
        foodBagId: UUID,
        dto: FoodBagDto,
    ): FoodBag {
        val now = clock.instant()

        return FoodBag(
            id = FoodBagId(foodBagId),
            storeId = StoreId(storeId),
            name = dto.name,
            description = dto.description,
            category = dto.category,
            originalPrice = dto.originalPrice,
            unitPrice = dto.unitPrice,
            allergens = dto.allergens,
            status = FoodBagStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
            version = dto.version,
        )
    }

    fun toDto(foodBag: FoodBag): FoodBagDto =
        FoodBagDto(
            name = foodBag.name,
            description = foodBag.description,
            category = foodBag.category,
            originalPrice = foodBag.originalPrice,
            unitPrice = foodBag.unitPrice,
            allergens = foodBag.allergens,
            version = foodBag.version,
        )

    fun toStatusDto(foodBag: FoodBag): FoodBagStatusDto =
        FoodBagStatusDto(
            status = foodBag.status,
            version = foodBag.version,
        )
}
