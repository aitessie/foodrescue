package com.example.foodrescue.offerservice.application.access

import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.NotFoundException
import com.example.foodrescue.offerservice.application.ports.CurrentUserPort
import com.example.foodrescue.offerservice.application.ports.PartnerStoreAccessPort
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus
import com.example.foodrescue.offerservice.domain.`enum`.StoreStatus
import org.springframework.stereotype.Component

@Component
class FoodBagAccessPolicy(
    private val currentUserPort: CurrentUserPort,
    private val partnerStoreAccessPort: PartnerStoreAccessPort,
) {
    fun checkAccess(
        partnerId: PartnerId,
        storeId: StoreId,
    ) {
        val userId = currentUserPort.getUserId()
        val access =
            partnerStoreAccessPort.checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = userId,
            )

        if (!access.storeBelongsToPartner) {
            throw NotFoundException(
                "Store ${storeId.value} was not found for " + "Partner ${partnerId.value}"
            )
        }

        if (access.partnerStatus != PartnerStatus.ACTIVE) {
            throw InvalidStateException("Partner ${partnerId.value} must be ACTIVE")
        }

        if (access.storeStatus != StoreStatus.ACTIVE) {
            throw InvalidStateException("Store ${storeId.value} must be ACTIVE")
        }

        if (currentUserPort.hasRole(ApplicationRole.ADMIN)) {
            return
        }

        val isAssignedManager =
            currentUserPort.hasRole(ApplicationRole.MANAGER) && access.userIsManager

        if (isAssignedManager) {
            return
        }

        val isAssignedStaff = currentUserPort.hasRole(ApplicationRole.STAFF) && access.userIsStaff

        if (isAssignedStaff) {
            return
        }

        throw AccessDeniedException("Current user cannot manage Store ${storeId.value}")
    }
}
