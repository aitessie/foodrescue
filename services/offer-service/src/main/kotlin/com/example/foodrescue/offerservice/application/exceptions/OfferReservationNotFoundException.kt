package com.example.foodrescue.offerservice.application.exceptions

import com.example.foodrescue.offerservice.domain.entities.ReservationId

class OfferReservationNotFoundException(id: ReservationId) :
    NotFoundException("OfferReservation '$id' was not found")
