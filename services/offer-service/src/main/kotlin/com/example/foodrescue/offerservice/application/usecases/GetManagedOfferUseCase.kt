package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.OfferAccessPolicy
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetManagedOfferUseCase(
    private val offerDBPort: OfferDBPort,
    private val accessPolicy: OfferAccessPolicy,
) {
    @Transactional(readOnly = true)
    fun execute(
        partnerId: PartnerId,
        storeId: StoreId,
        offerId: OfferId,
    ): Offer {
        val offer = offerDBPort.findById(offerId) ?: throw OfferNotFoundException(offerId)

        if (offer.storeId != storeId) {
            throw OfferNotFoundException(offerId)
        }

        accessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = storeId,
        )

        return offer
    }
}
