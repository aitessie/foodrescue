package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.StoreAccessPolicy
import com.example.foodrescue.partnerservice.application.exceptions.StoreNotFoundException
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import org.springframework.stereotype.Service

@Service
class GetStoreUseCase(
    private val storeDBPort: StoreDBPort,
    private val storeAccessPolicy: StoreAccessPolicy,
) {
    fun getStore(
        partnerId: PartnerId,
        storeId: StoreId,
    ): Store {
        val store = storeDBPort.findById(storeId) ?: throw StoreNotFoundException(storeId)

        if (store.partnerId != partnerId) {
            throw StoreNotFoundException(storeId)
        }

        storeAccessPolicy.checkAccess(
            action = AccessAction.READ,
            resource = store,
        )

        return store
    }
}
