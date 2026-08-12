package com.example.foodrescue.offerservice.domain.entity

import com.example.foodrescue.offerservice.domain.enum.Allergen
import com.example.foodrescue.offerservice.domain.enum.MoneyCurrency
import com.example.foodrescue.offerservice.domain.enum.ProductCategory
import com.example.foodrescue.offerservice.domain.enum.ProductTemplateStatus
import java.time.Instant

class ProductTemplate(
    val id: ProductTemplateId,
    val partnerId: PartnerId,
    val storeId: StoreId,
    name: String,
    description: String?,
    category: ProductCategory,
    originalPrice: Money,
    unitPrice: Money,
    allergens: Set<Allergen>,
    status: ProductTemplateStatus,
    val createdAt: Instant,
    updatedAt: Instant,
    version: Long,
) {
    var name: String = validateProductName(name)
        private set

    var description: String? = description
        private set

    var category: ProductCategory = category
        private set

    var originalPrice: Money = validateRubPrice(originalPrice, "originalPrice")
        private set

    var unitPrice: Money = validateDiscountPrice(this.originalPrice, unitPrice)
        private set

    private var allergenValues: Set<Allergen> = allergens.toSet()

    var allergens: Set<Allergen>
        get() = allergenValues.toSet()
        private set(value) {
            allergenValues = value.toSet()
        }

    var status: ProductTemplateStatus = status
        private set

    var updatedAt: Instant = validateInitialProductTimestamp(createdAt, updatedAt)
        private set

    val version: Long = validateProductVersion(version)

    fun updateFrom(
        source: ProductTemplate,
        updatedAt: Instant,
    ) {
        require(source.id == id) {
            "Product template id cannot be changed"
        }
        require(source.partnerId == partnerId) {
            "Product template partnerId cannot be changed"
        }
        require(source.storeId == storeId) {
            "Product template storeId cannot be changed"
        }
        check(
            status != ProductTemplateStatus.INACTIVE ||
                source.status != ProductTemplateStatus.ACTIVE
        ) {
            "Inactive product template cannot be reactivated"
        }

        validateUpdateTimestamp(updatedAt)

        name = validateProductName(source.name)
        description = source.description
        category = source.category
        originalPrice = validateRubPrice(source.originalPrice, "originalPrice")
        unitPrice = validateDiscountPrice(originalPrice, source.unitPrice)
        allergens = source.allergens
        status = source.status
        this.updatedAt = updatedAt
    }

    fun deactivate(updatedAt: Instant) {
        validateUpdateTimestamp(updatedAt)

        if (status == ProductTemplateStatus.INACTIVE) {
            return
        }

        status = ProductTemplateStatus.INACTIVE
        this.updatedAt = updatedAt
    }

    private fun validateUpdateTimestamp(candidate: Instant) {
        require(!candidate.isBefore(createdAt)) {
            "Product template updatedAt cannot be earlier than createdAt"
        }
        require(!candidate.isBefore(updatedAt)) {
            "Product template updatedAt cannot move backwards"
        }
    }
}

private fun validateProductName(value: String): String {
    val normalized = value.trim()

    require(normalized.isNotEmpty()) {
        "Product template name must not be blank"
    }
    require(normalized.length <= 255) {
        "Product template name must contain at most 255 characters"
    }

    return normalized
}

private fun validateRubPrice(
    value: Money,
    fieldName: String,
): Money {
    require(value.currency == MoneyCurrency.RUB) {
        "Product template $fieldName currency must be RUB"
    }

    return value
}

private fun validateDiscountPrice(
    originalPrice: Money,
    unitPrice: Money,
): Money {
    validateRubPrice(unitPrice, "unitPrice")
    require(unitPrice.amountMinor < originalPrice.amountMinor) {
        "Product template unitPrice must be lower than originalPrice"
    }

    return unitPrice
}

private fun validateInitialProductTimestamp(
    createdAt: Instant,
    updatedAt: Instant,
): Instant {
    require(!updatedAt.isBefore(createdAt)) {
        "Product template updatedAt cannot be earlier than createdAt"
    }

    return updatedAt
}

private fun validateProductVersion(version: Long): Long {
    require(version >= 0) {
        "Product template version must not be negative"
    }

    return version
}
