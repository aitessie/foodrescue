package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.`enum`.Allergen
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import java.time.Instant

class Offer(
    val id: OfferId,
    val storeId: StoreId,
    val foodBagId: FoodBagId,
    val category: FoodBagCategory,
    val unitPrice: Money,
    allergens: Set<Allergen>,
    status: OfferStatus,
    totalQuantity: Int,
    availableQuantity: Int,
    pickupWindow: PickupWindow,
    val createdAt: Instant,
    updatedAt: Instant,
    version: Long,
) {
    private val allergenValues: Set<Allergen> = allergens.toSet()

    val allergens: Set<Allergen>
        get() = allergenValues.toSet()

    var status: OfferStatus = status
        private set

    var totalQuantity: Int = validateTotalQuantity(totalQuantity)
        private set

    var availableQuantity: Int =
        validateAvailableQuantity(
            totalQuantity = this.totalQuantity,
            availableQuantity = availableQuantity,
        )
        private set

    val reservedQuantity: Int
        get() = totalQuantity - availableQuantity

    var pickupWindow: PickupWindow = pickupWindow
        private set

    var updatedAt: Instant =
        validateUpdatedAt(
            currentUpdatedAt = createdAt,
            requestedUpdatedAt = updatedAt,
        )
        private set

    val version: Long = validateVersion(version)

    fun updateFrom(
        source: Offer,
        updatedAt: Instant,
    ) {
        validateUpdateSource(source)
        validateUpdatedAt(
            currentUpdatedAt = this.updatedAt,
            requestedUpdatedAt = updatedAt,
        )

        val updatedTotalQuantity = validateTotalQuantity(source.totalQuantity)
        val currentReservedQuantity = reservedQuantity

        check(updatedTotalQuantity >= currentReservedQuantity) {
            "Offer totalQuantity cannot be less than reservedQuantity"
        }

        totalQuantity = updatedTotalQuantity
        availableQuantity = updatedTotalQuantity - currentReservedQuantity
        pickupWindow = source.pickupWindow
        this.updatedAt = updatedAt
    }

    fun changeStatus(
        targetStatus: OfferStatus,
        foodBagStatus: FoodBagStatus,
        now: Instant,
        updatedAt: Instant,
    ): Boolean {
        if (status == targetStatus) {
            return false
        }

        validateUpdatedAt(
            currentUpdatedAt = this.updatedAt,
            requestedUpdatedAt = updatedAt,
        )
        validateStatusTransition(
            targetStatus = targetStatus,
            foodBagStatus = foodBagStatus,
            now = now,
        )

        status = targetStatus
        this.updatedAt = updatedAt

        return true
    }

    fun changeTotalQuantity(
        quantity: Int,
        updatedAt: Instant,
    ) {
        val validatedQuantity = validateTotalQuantity(quantity)
        val currentReservedQuantity = reservedQuantity

        check(validatedQuantity >= currentReservedQuantity) {
            "Offer totalQuantity cannot be less than reservedQuantity"
        }

        validateUpdatedAt(
            currentUpdatedAt = this.updatedAt,
            requestedUpdatedAt = updatedAt,
        )

        totalQuantity = validatedQuantity
        availableQuantity = validatedQuantity - currentReservedQuantity
        this.updatedAt = updatedAt
    }

    fun reserve(
        quantity: Int,
        now: Instant,
        updatedAt: Instant,
    ) {
        validateReservationQuantity(quantity)

        check(status == OfferStatus.ACTIVE) {
            "FoodBags can only be reserved from an ACTIVE Offer"
        }
        check(pickupWindow.end.isAfter(now)) {
            "FoodBags cannot be reserved after pickup window end"
        }
        check(quantity <= availableQuantity) {
            "Requested quantity exceeds available Offer quantity"
        }

        validateUpdatedAt(
            currentUpdatedAt = this.updatedAt,
            requestedUpdatedAt = updatedAt,
        )

        availableQuantity -= quantity
        this.updatedAt = updatedAt
    }

    fun release(
        quantity: Int,
        updatedAt: Instant,
    ) {
        validateReservationQuantity(quantity)

        check(quantity <= reservedQuantity) {
            "Released quantity exceeds reserved Offer quantity"
        }

        validateUpdatedAt(
            currentUpdatedAt = this.updatedAt,
            requestedUpdatedAt = updatedAt,
        )

        availableQuantity += quantity
        this.updatedAt = updatedAt
    }

    fun closeWhenExpired(
        now: Instant,
        updatedAt: Instant,
    ): Boolean {
        if (isTerminal()) {
            return false
        }

        if (pickupWindow.end.isAfter(now)) {
            return false
        }

        validateUpdatedAt(
            currentUpdatedAt = this.updatedAt,
            requestedUpdatedAt = updatedAt,
        )

        status = OfferStatus.CLOSED
        this.updatedAt = updatedAt

        return true
    }

    private fun validateUpdateSource(source: Offer) {
        require(source.id == id) {
            "Offer id cannot be changed"
        }
        require(source.storeId == storeId) {
            "Offer storeId cannot be changed"
        }
        require(source.foodBagId == foodBagId) {
            "Offer foodBagId cannot be changed"
        }
        require(source.category == category) {
            "Offer category snapshot cannot be changed"
        }
        require(source.unitPrice == unitPrice) {
            "Offer unitPrice snapshot cannot be changed"
        }
        require(source.allergens == allergens) {
            "Offer allergens snapshot cannot be changed"
        }
        require(source.status == status) {
            "Offer status must be changed through the status subresource"
        }
    }

    private fun validateStatusTransition(
        targetStatus: OfferStatus,
        foodBagStatus: FoodBagStatus,
        now: Instant,
    ) {
        if (targetStatus == OfferStatus.SCHEDULED) {
            throw IllegalStateException("Offer cannot return to SCHEDULED status")
        }

        if (targetStatus == OfferStatus.CLOSED) {
            throw IllegalStateException("CLOSED status is managed by Offer Service")
        }

        if (targetStatus == OfferStatus.ACTIVE) {
            validateActivation(
                foodBagStatus = foodBagStatus,
                now = now,
            )
            return
        }

        if (targetStatus == OfferStatus.CANCELLED) {
            validateCancellation()
            return
        }

        throw IllegalStateException("Unsupported Offer status transition")
    }

    private fun validateActivation(
        foodBagStatus: FoodBagStatus,
        now: Instant,
    ) {
        check(status == OfferStatus.SCHEDULED) {
            "Only SCHEDULED Offer can become ACTIVE"
        }
        check(foodBagStatus == FoodBagStatus.ACTIVE) {
            "Offer cannot become ACTIVE for an inactive FoodBag"
        }
        check(pickupWindow.end.isAfter(now)) {
            "Offer cannot become ACTIVE after pickup window end"
        }
        check(availableQuantity > 0) {
            "Offer without available FoodBags cannot become ACTIVE"
        }
    }

    private fun validateCancellation() {
        val canBeCancelled = status == OfferStatus.SCHEDULED || status == OfferStatus.ACTIVE

        check(canBeCancelled) {
            "Only SCHEDULED or ACTIVE Offer can be cancelled"
        }
        check(reservedQuantity == 0) {
            "Offer with active reservations cannot be cancelled"
        }
    }

    private fun isTerminal(): Boolean =
        status == OfferStatus.CANCELLED || status == OfferStatus.CLOSED

    private fun validateTotalQuantity(quantity: Int): Int {
        require(quantity > 0) {
            "Offer totalQuantity must be greater than zero"
        }

        return quantity
    }

    private fun validateAvailableQuantity(
        totalQuantity: Int,
        availableQuantity: Int,
    ): Int {
        require(availableQuantity >= 0) {
            "Offer availableQuantity must not be negative"
        }
        require(availableQuantity <= totalQuantity) {
            "Offer availableQuantity must not exceed totalQuantity"
        }

        return availableQuantity
    }

    private fun validateReservationQuantity(quantity: Int) {
        require(quantity > 0) {
            "Reservation quantity must be greater than zero"
        }
    }

    private fun validateUpdatedAt(
        currentUpdatedAt: Instant,
        requestedUpdatedAt: Instant,
    ): Instant {
        require(!requestedUpdatedAt.isBefore(createdAt)) {
            "Offer updatedAt must not be earlier than createdAt"
        }
        require(!requestedUpdatedAt.isBefore(currentUpdatedAt)) {
            "Offer updatedAt must not move backwards"
        }

        return requestedUpdatedAt
    }

    private fun validateVersion(version: Long): Long {
        require(version >= 0) {
            "Offer version must not be negative"
        }

        return version
    }
}
