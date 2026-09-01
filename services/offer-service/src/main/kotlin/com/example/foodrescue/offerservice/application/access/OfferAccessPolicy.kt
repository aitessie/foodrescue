package com.example.foodrescue.offerservice.application.access

import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.PartnerStoreNotFoundException
import com.example.foodrescue.offerservice.application.ports.CurrentUserPort
import com.example.foodrescue.offerservice.application.ports.PartnerStoreAccessPort
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.enum.ApplicationRole
import com.example.foodrescue.offerservice.domain.enum.PartnerStatus
import com.example.foodrescue.offerservice.domain.enum.StoreStatus
import org.springframework.stereotype.Component

@Component
class OfferAccessPolicy(
    private val currentUserPort: CurrentUserPort,
    private val partnerStoreAccessPort: PartnerStoreAccessPort,
) {
    fun checkAccess(
        partnerId: PartnerId,
        storeId: StoreId,
    ) {
        val userId = currentUserPort.getUserId()
        val snapshot =
            partnerStoreAccessPort.checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = userId,
            )

        if (!snapshot.storeBelongsToPartner) {
            throw PartnerStoreNotFoundException(partnerId, storeId)
        }
        if (snapshot.partnerStatus != PartnerStatus.ACTIVE) {
            throw InvalidStateException("Partner '$partnerId' is not active")
        }
        if (snapshot.storeStatus != StoreStatus.ACTIVE) {
            throw InvalidStateException("Store '$storeId' is not active")
        }

        val hasAccess =
            currentUserPort.hasRole(ApplicationRole.ADMIN) ||
                (currentUserPort.hasRole(ApplicationRole.MANAGER) && snapshot.userIsManager) ||
                (currentUserPort.hasRole(ApplicationRole.STAFF) && snapshot.userIsStaff)

        if (!hasAccess) {
            throw AccessDeniedException()
        }
    }

    fun checkOwnership(
        offer: Offer,
        storeId: StoreId,
        offerId: OfferId,
    ) {
        if (offer.id != offerId || offer.storeId != storeId) {
            throw OfferNotFoundException(offerId)
        }
    }
}
