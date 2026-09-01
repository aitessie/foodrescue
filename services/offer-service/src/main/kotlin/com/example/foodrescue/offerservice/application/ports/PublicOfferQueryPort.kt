package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferSearchFilter
import com.example.foodrescue.offerservice.domain.entities.OfferSearchItem
import com.example.foodrescue.offerservice.domain.entities.OfferSearchPage
import java.time.Instant

interface PublicOfferQueryPort {
    fun findVisibleById(
        offerId: OfferId,
        visibleAt: Instant,
    ): OfferSearchItem?

    fun findVisiblePage(
        filter: OfferSearchFilter,
        visibleAt: Instant,
    ): OfferSearchPage
}
