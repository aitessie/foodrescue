package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.`enum`.Allergen
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import java.time.Instant

class FoodBag(
    val id: FoodBagId,
    val storeId: StoreId,
    name: String,
    description: String?,
    category: FoodBagCategory,
    originalPrice: Money,
    unitPrice: Money,
    allergens: Set<Allergen>,
    status: FoodBagStatus,
    val createdAt: Instant,
    updatedAt: Instant,
    version: Long,
) {
    var name: String = validateFoodBagName(name)
        private set

    var description: String? = normalizeFoodBagDescription(description)
        private set

    var category: FoodBagCategory = category
        private set

    var originalPrice: Money = originalPrice.also {
        validateFoodBagPrices(
            originalPrice = it,
            unitPrice = unitPrice,
        )
    }
        private set

    var unitPrice: Money = unitPrice.also {
        validateFoodBagPrices(
            originalPrice = originalPrice,
            unitPrice = it,
        )
    }
        private set

    private var allergenValues: Set<Allergen> = allergens.toSet()

    val allergens: Set<Allergen>
        get() = allergenValues.toSet()

    var status: FoodBagStatus = status
        private set

    var updatedAt: Instant =
        validateFoodBagUpdatedAt(
            createdAt = createdAt,
            currentUpdatedAt = createdAt,
            requestedUpdatedAt = updatedAt,
        )
        private set

    val version: Long = validateFoodBagVersion(version)

    fun updateFrom(
        source: FoodBag,
        updatedAt: Instant,
    ) {
        require(source.id == id) {
            "FoodBag id cannot be changed"
        }
        require(source.storeId == storeId) {
            "FoodBag storeId cannot be changed"
        }
        require(source.status == status) {
            "FoodBag status must be changed through the status subresource"
        }

        validateFoodBagPrices(
            originalPrice = source.originalPrice,
            unitPrice = source.unitPrice,
        )
        validateFoodBagUpdatedAt(
            createdAt = createdAt,
            currentUpdatedAt = this.updatedAt,
            requestedUpdatedAt = updatedAt,
        )

        name = validateFoodBagName(source.name)
        description = normalizeFoodBagDescription(source.description)
        category = source.category
        originalPrice = source.originalPrice
        unitPrice = source.unitPrice
        allergenValues = source.allergens.toSet()
        this.updatedAt = updatedAt
    }

    fun changeStatus(
        targetStatus: FoodBagStatus,
        updatedAt: Instant,
    ): Boolean {
        validateFoodBagUpdatedAt(
            createdAt = createdAt,
            currentUpdatedAt = this.updatedAt,
            requestedUpdatedAt = updatedAt,
        )

        if (status == targetStatus) {
            return false
        }

        status = targetStatus
        this.updatedAt = updatedAt

        return true
    }

    private fun validateFoodBagName(name: String): String {
        val normalizedName = name.trim()

        require(normalizedName.isNotEmpty()) {
            "FoodBag name must not be blank"
        }
        require(normalizedName.length <= 255) {
            "FoodBag name must not exceed 255 characters"
        }

        return normalizedName
    }

    private fun normalizeFoodBagDescription(description: String?): String? =
        description?.trim()?.takeIf { value -> value.isNotEmpty() }

    private fun validateFoodBagPrices(
        originalPrice: Money,
        unitPrice: Money,
    ) {
        require(originalPrice.currency == MoneyCurrency.RUB) {
            "FoodBag originalPrice currency must be RUB"
        }
        require(unitPrice.currency == MoneyCurrency.RUB) {
            "FoodBag unitPrice currency must be RUB"
        }
        require(unitPrice.amountMinor < originalPrice.amountMinor) {
            "FoodBag unitPrice must be less than originalPrice"
        }
    }

    private fun validateFoodBagUpdatedAt(
        createdAt: Instant,
        currentUpdatedAt: Instant,
        requestedUpdatedAt: Instant,
    ): Instant {
        require(!requestedUpdatedAt.isBefore(createdAt)) {
            "FoodBag updatedAt must not be earlier than createdAt"
        }
        require(!requestedUpdatedAt.isBefore(currentUpdatedAt)) {
            "FoodBag updatedAt must not move backwards"
        }

        return requestedUpdatedAt
    }

    private fun validateFoodBagVersion(version: Long): Long {
        require(version >= 0) {
            "FoodBag version must not be negative"
        }

        return version
    }
}
