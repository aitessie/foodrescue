package com.example.foodrescue.partnerservice.application.access

import com.example.foodrescue.partnerservice.application.exceptions.StoreAccessDeniedException
import com.example.foodrescue.partnerservice.application.ports.CurrentUserPort
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreStaffDBPort
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.ApplicationRole
import org.springframework.stereotype.Service

@Service
class StoreAccessPolicy(
    private val partnerDBPort: PartnerDBPort,
    private val storeStaffDBPort: StoreStaffDBPort,
    private val currentUserPort: CurrentUserPort,
) : AccessPolicy<Store> {
    override fun checkAccess(
        action: AccessAction,
        resource: Store,
    ) {
        val userId = currentUserPort.getUserId()

        val accessAllowed =
            when (action) {
                AccessAction.READ -> {
                    canReadStore(
                        store = resource,
                        userId = userId,
                    )
                }

                AccessAction.CREATE_OR_UPDATE -> {
                    canModifyStore(
                        store = resource,
                        userId = userId,
                    )
                }
            }

        if (!accessAllowed) {
            throw StoreAccessDeniedException()
        }
    }

    private fun canReadStore(
        store: Store,
        userId: String,
    ): Boolean =
        currentUserPort.hasRole(ApplicationRole.ADMIN) ||
            (currentUserPort.hasRole(ApplicationRole.MANAGER) &&
                isPartnerManager(
                    partnerId = store.partnerId,
                    userId = userId,
                )) ||
            (currentUserPort.hasRole(ApplicationRole.STAFF) &&
                storeStaffDBPort.isStaffAssignedToStore(
                    userId = userId,
                    storeId = store.id,
                ))

    private fun canModifyStore(
        store: Store,
        userId: String,
    ): Boolean =
        currentUserPort.hasRole(ApplicationRole.ADMIN) ||
            (currentUserPort.hasRole(ApplicationRole.MANAGER) &&
                isPartnerManager(
                    partnerId = store.partnerId,
                    userId = userId,
                ))

    private fun isPartnerManager(
        partnerId: PartnerId,
        userId: String,
    ): Boolean {
        val partner = partnerDBPort.findById(partnerId) ?: return false

        return partner.managerId == userId
    }
}
