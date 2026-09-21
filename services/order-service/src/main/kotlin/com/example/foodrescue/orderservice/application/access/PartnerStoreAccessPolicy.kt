package com.example.foodrescue.orderservice.application.access

import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.PickupAccessDeniedException
import com.example.foodrescue.orderservice.application.ports.CurrentUserPort
import com.example.foodrescue.orderservice.application.ports.PartnerStoreAccessPort
import com.example.foodrescue.orderservice.domain.entities.StoreId
import com.example.foodrescue.orderservice.domain.enum.ApplicationRole
import com.example.foodrescue.orderservice.domain.enum.PartnerStatus
import com.example.foodrescue.orderservice.domain.enum.StoreStatus
import org.springframework.stereotype.Service

@Service
class PartnerStoreAccessPolicy(
    private val currentUserPort: CurrentUserPort,
    private val partnerStoreAccessPort: PartnerStoreAccessPort,
) {
    fun checkAccess(storeId: StoreId) {
        val userId = currentUserPort.getUserId()
        val access =
            partnerStoreAccessPort.checkAccess(
                storeId = storeId,
                userId = userId,
            )

        if (access.partnerStatus != PartnerStatus.ACTIVE) {
            throw OrderConflictException("Partner is not active")
        }
        if (access.storeStatus != StoreStatus.ACTIVE) {
            throw OrderConflictException("Store is not active")
        }

        val accessAllowed =
            currentUserPort.hasRole(ApplicationRole.ADMIN) ||
                (currentUserPort.hasRole(ApplicationRole.MANAGER) && access.userIsStoreManager) ||
                (currentUserPort.hasRole(ApplicationRole.STAFF) && access.userIsStoreStaff)

        if (!accessAllowed) {
            throw PickupAccessDeniedException()
        }
    }
}
