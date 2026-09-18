package com.example.foodrescue.offerservice.application.events

import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.ReservationId

class OfferReservationRejectedEventPayload(
    val reservationId: ReservationId,
    val offerId: OfferId,
    val customerId: String,
    val quantity: Int,
    val reason: String,
) : ApplicationEventPayload
