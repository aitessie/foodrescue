package com.example.foodrescue.partnerservice.application.access

import com.example.foodrescue.partnerservice.application.exception.PartnerAccessDeniedException
import com.example.foodrescue.partnerservice.application.ports.CurrentUserPort
import com.example.foodrescue.partnerservice.application.ports.StoreStaffDBPort
import com.example.foodrescue.partnerservice.domain.entity.Partner
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.ApplicationRole
import org.springframework.stereotype.Service

@Service
class PartnerAccessPolicy(
    private val storeStaffDBPort: StoreStaffDBPort,
    private val currentUserPort: CurrentUserPort,
) : AccessPolicy<Partner> {
    override fun checkAccess(
        action: AccessAction,
        resource: Partner,
    ) {
        val userId = currentUserPort.getUserId()

        val accessAllowed =
            when (action) {
                AccessAction.READ -> {
                    canReadPartner(
                        partner = resource,
                        userId = userId,
                    )
                }

                AccessAction.CREATE_OR_UPDATE -> {
                    canModifyPartner(
                        partner = resource,
                        userId = userId,
                    )
                }
            }

        if (!accessAllowed) {
            throw PartnerAccessDeniedException()
        }
    }

    private fun canReadPartner(
        partner: Partner,
        userId: String,
    ): Boolean =
        currentUserPort.hasRole(ApplicationRole.ADMIN) ||
            (currentUserPort.hasRole(ApplicationRole.MANAGER) && partner.managerId == userId) ||
            (currentUserPort.hasRole(ApplicationRole.STAFF) &&
                storeStaffDBPort.isStaffAssignedToAnyStoreOfPartner(
                    userId = userId,
                    partnerId = partner.id,
                ))

    private fun canModifyPartner(
        partner: Partner,
        userId: String,
    ): Boolean =
        currentUserPort.hasRole(ApplicationRole.ADMIN) ||
            (currentUserPort.hasRole(ApplicationRole.MANAGER) && partner.managerId == userId)
}
