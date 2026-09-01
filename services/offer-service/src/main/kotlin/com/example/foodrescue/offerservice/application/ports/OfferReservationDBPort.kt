package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.ReservationId

interface OfferReservationDBPort {
    fun findById(id: ReservationId): OfferReservation?

    fun save(reservation: OfferReservation): OfferReservation
}
