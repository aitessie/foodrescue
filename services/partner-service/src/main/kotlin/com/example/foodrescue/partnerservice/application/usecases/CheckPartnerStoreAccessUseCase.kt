package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.exceptions.PartnerNotFoundException
import com.example.foodrescue.partnerservice.application.exceptions.StoreNotFoundException
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreStaffDBPort
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.PartnerStoreAccessSnapshot
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import org.springframework.stereotype.Service

@Service
class CheckPartnerStoreAccessUseCase(
    private val partnerDBPort: PartnerDBPort,
    private val storeDBPort: StoreDBPort,
    private val storeStaffDBPort: StoreStaffDBPort,
) {
    fun execute(
        partnerId: PartnerId? = null,
        storeId: StoreId,
        userId: String,
    ): PartnerStoreAccessSnapshot {
        val store = storeDBPort.findById(storeId) ?: throw StoreNotFoundException(storeId)

        if (partnerId != null && store.partnerId != partnerId) {
            throw StoreNotFoundException(storeId)
        }

        val actualPartnerId = store.partnerId
        val partner =
            partnerDBPort.findById(actualPartnerId) ?: throw PartnerNotFoundException(actualPartnerId)

        return PartnerStoreAccessSnapshot(
            partnerStatus = partner.status,
            storeStatus = store.status,
            userIsStoreManager = partner.managerId == userId,
            userIsStoreStaff =
                storeStaffDBPort.isStaffAssignedToStore(
                    userId = userId,
                    storeId = storeId,
                ),
        )
    }
}
