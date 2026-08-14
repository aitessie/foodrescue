package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PartnerStoreAccessSnapshot
import com.example.foodrescue.offerservice.domain.entities.StoreId

interface PartnerStoreAccessPort {
    fun checkAccess(
        partnerId: PartnerId,
        storeId: StoreId,
        userId: String,
    ): PartnerStoreAccessSnapshot
}
