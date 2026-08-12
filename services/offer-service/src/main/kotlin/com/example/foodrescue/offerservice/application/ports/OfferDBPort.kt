package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entity.NormalizedOfferSearchFilter
import com.example.foodrescue.offerservice.domain.entity.Offer
import com.example.foodrescue.offerservice.domain.entity.OfferCursorData
import com.example.foodrescue.offerservice.domain.entity.OfferId
import com.example.foodrescue.offerservice.domain.entity.OfferSearchItem
import java.time.Instant

interface OfferDBPort {
    fun findById(id: OfferId): Offer?

    fun findPublicById(id: OfferId, visibleAt: Instant): OfferSearchItem?

    fun save(offer: Offer): Offer

    fun saveAll(offers: List<Offer>): List<Offer>

    fun search(
        filter: NormalizedOfferSearchFilter,
        cursor: OfferCursorData?,
        resultLimit: Int,
        visibleAt: Instant,
    ): List<OfferSearchItem>

    fun findExpiredBatch(expiredAt: Instant, batchSize: Int): List<Offer>
}
