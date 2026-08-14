package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.enum.Allergen
import com.example.foodrescue.offerservice.domain.enum.MoneyCurrency
import com.example.foodrescue.offerservice.domain.enum.OfferStatus
import com.example.foodrescue.offerservice.domain.enum.ProductCategory
import java.time.Instant

class Offer(
    val id: OfferId,
    val partnerId: PartnerId,
    val storeId: StoreId,
    val productTemplateId: ProductTemplateId,
    val category: ProductCategory,
    unitPrice: Money,
    allergens: Set<Allergen>,
    status: OfferStatus,
    totalQuantity: Int,
    pickupWindow: PickupWindow,
    val createdAt: Instant,
    updatedAt: Instant,
    version: Long,
) {
    val unitPrice: Money = validateOfferUnitPrice(unitPrice)

    private val allergenValues: Set<Allergen> = allergens.toSet()

    val allergens: Set<Allergen>
        get() = allergenValues.toSet()

    var status: OfferStatus = status
        private set

    var totalQuantity: Int = validateTotalQuantity(totalQuantity)
        private set

    var pickupWindow: PickupWindow = pickupWindow
        private set

    var updatedAt: Instant = validateInitialOfferTimestamp(createdAt, updatedAt)
        private set

    val version: Long = validateOfferVersion(version)

    val availableQuantity: Int
        get() = totalQuantity

    fun updateFrom(
        source: Offer,
        updatedAt: Instant,
    ) {
        require(source.id == id) {
            "Offer id cannot be changed"
        }
        require(source.partnerId == partnerId) {
            "Offer partnerId cannot be changed"
        }
        require(source.storeId == storeId) {
            "Offer storeId cannot be changed"
        }
        require(source.productTemplateId == productTemplateId) {
            "Offer productTemplateId cannot be changed"
        }
        require(source.category == category) {
            "Offer category snapshot cannot be changed"
        }
        require(source.unitPrice == unitPrice) {
            "Offer unitPrice snapshot cannot be changed"
        }
        require(source.allergens == allergens) {
            "Offer allergen snapshot cannot be changed"
        }
        check(source.status == status) {
            "Offer status cannot be changed through update"
        }
        check(!status.isTerminal()) {
            "Terminal offer cannot be updated"
        }

        validateUpdateTimestamp(updatedAt)

        totalQuantity = validateTotalQuantity(source.totalQuantity)
        pickupWindow = source.pickupWindow
        this.updatedAt = updatedAt
    }

    fun activate(
        now: Instant,
        updatedAt: Instant,
    ) {
        check(status == OfferStatus.SCHEDULED) {
            "Only a scheduled offer can be activated"
        }
        check(pickupWindow.end.isAfter(now)) {
            "Offer with an expired pickup window cannot be activated"
        }

        validateUpdateTimestamp(updatedAt)

        status = OfferStatus.ACTIVE
        this.updatedAt = updatedAt
    }

    fun changeTotalQuantity(
        quantity: Int,
        updatedAt: Instant,
    ) {
        val validatedQuantity = validateTotalQuantity(quantity)

        check(!status.isTerminal()) {
            "Terminal offer quantity cannot be changed"
        }

        validateUpdateTimestamp(updatedAt)

        totalQuantity = validatedQuantity
        this.updatedAt = updatedAt
    }

    fun cancel(updatedAt: Instant) {
        check(
            status == OfferStatus.DRAFT ||
                status == OfferStatus.SCHEDULED ||
                status == OfferStatus.ACTIVE ||
                status == OfferStatus.SOLD_OUT
        ) {
            "Offer cannot be cancelled from status ${status.code}"
        }

        validateUpdateTimestamp(updatedAt)

        status = OfferStatus.CANCELLED
        this.updatedAt = updatedAt
    }

    fun closeWhenExpired(
        now: Instant,
        updatedAt: Instant,
    ): Boolean {
        if (status.isTerminal()) {
            return false
        }
        if (pickupWindow.end.isAfter(now)) {
            return false
        }

        validateUpdateTimestamp(updatedAt)

        status = OfferStatus.CLOSED
        this.updatedAt = updatedAt
        return true
    }

    private fun validateUpdateTimestamp(candidate: Instant) {
        require(!candidate.isBefore(createdAt)) {
            "Offer updatedAt cannot be earlier than createdAt"
        }
        require(!candidate.isBefore(updatedAt)) {
            "Offer updatedAt cannot move backwards"
        }
    }
}

private fun validateOfferUnitPrice(value: Money): Money {
    require(value.currency == MoneyCurrency.RUB) {
        "Offer unitPrice currency must be RUB"
    }

    return value
}

private fun validateTotalQuantity(value: Int): Int {
    require(value > 0) {
        "Offer totalQuantity must be greater than zero"
    }

    return value
}

private fun validateInitialOfferTimestamp(
    createdAt: Instant,
    updatedAt: Instant,
): Instant {
    require(!updatedAt.isBefore(createdAt)) {
        "Offer updatedAt cannot be earlier than createdAt"
    }

    return updatedAt
}

private fun validateOfferVersion(version: Long): Long {
    require(version >= 0) {
        "Offer version must not be negative"
    }

    return version
}

private fun OfferStatus.isTerminal(): Boolean =
    this == OfferStatus.CANCELLED || this == OfferStatus.CLOSED
