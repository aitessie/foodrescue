package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entity.PartnerId
import com.example.foodrescue.offerservice.domain.entity.PartnerStoreAccessSnapshot
import com.example.foodrescue.offerservice.domain.entity.StoreId

interface PartnerStoreAccessPort {
    fun checkAccess(
        partnerId: PartnerId,
        storeId: StoreId,
        userId: String,
    ): PartnerStoreAccessSnapshot
}
