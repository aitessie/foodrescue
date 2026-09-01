package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import java.time.Instant

interface OfferDBPort {
    fun findById(id: OfferId): Offer?

    fun save(offer: Offer): Offer

    fun findExpiredBatch(expiredAt: Instant, batchSize: Int): List<Offer>
}
