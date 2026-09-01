package com.example.foodrescue.offerservice.application.events

import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
import java.time.Instant

data class OfferReservationEventPayload(
    val reservationId: ReservationId,
    val offerId: OfferId,
    val quantity: Int,
    val reservationStatus: ReservationStatus,
    val offerTotalQuantity: Int,
    val offerAvailableQuantity: Int,
    val offerReservedQuantity: Int,
    val reservationCreatedAt: Instant,
    val reservationUpdatedAt: Instant,
) : ApplicationEventPayload
