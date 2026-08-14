package com.example.foodrescue.partnerservice.application.ports

import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.StoreId

interface StoreStaffDBPort {
    fun isStaffAssignedToStore(
        userId: String,
        storeId: StoreId,
    ): Boolean

    fun isStaffAssignedToAnyStoreOfPartner(
        userId: String,
        partnerId: PartnerId,
    ): Boolean
}
