package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
import java.time.Instant

class OfferReservation(
    val id: ReservationId,
    val offerId: OfferId,
    val customerId: String,
    val quantity: Int,
    status: ReservationStatus,
    val createdAt: Instant,
    updatedAt: Instant,
    val version: Long,
) {
    var status: ReservationStatus = status
        private set

    var updatedAt: Instant = updatedAt
        private set

    init {
        validateCustomerId(customerId)
        validateQuantity(quantity)
        validateVersion(version)
        validateUpdatedAt(
            updatedAt = updatedAt,
            earliestAllowed = createdAt,
        )
    }

    fun release(updatedAt: Instant): Boolean {
        validateUpdatedAt(
            updatedAt = updatedAt,
            earliestAllowed = this.updatedAt,
        )

        if (status == ReservationStatus.RELEASED) {
            return false
        }

        check(status == ReservationStatus.RESERVED) {
            "Only a reserved reservation can be released"
        }

        status = ReservationStatus.RELEASED
        this.updatedAt = updatedAt

        return true
    }

    private fun validateCustomerId(customerId: String) {
        require(customerId.isNotBlank()) {
            "Reservation customerId must not be blank"
        }
    }

    private fun validateQuantity(quantity: Int) {
        require(quantity > 0) {
            "Reservation quantity must be greater than zero"
        }
    }

    private fun validateVersion(version: Long) {
        require(version >= 0) {
            "Reservation version must not be negative"
        }
    }

    private fun validateUpdatedAt(
        updatedAt: Instant,
        earliestAllowed: Instant,
    ) {
        require(!updatedAt.isBefore(earliestAllowed)) {
            "Reservation updatedAt must not move backwards"
        }
    }
}
