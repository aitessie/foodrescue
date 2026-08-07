package com.example.foodrescue.partnerservice.application.ports

import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import com.example.foodrescue.partnerservice.domain.entity.StoreId

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
