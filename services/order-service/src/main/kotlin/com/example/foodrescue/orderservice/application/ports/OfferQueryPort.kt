package com.example.foodrescue.orderservice.application.ports

import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.OfferSnapshot

interface OfferQueryPort {
    fun getOffer(offerId: OfferId): OfferSnapshot
}
