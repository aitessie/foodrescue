package com.example.foodrescue.orderservice.application.ports

import com.example.foodrescue.orderservice.domain.entities.PartnerStoreAccessSnapshot
import com.example.foodrescue.orderservice.domain.entities.StoreId

interface PartnerStoreAccessPort {
    fun checkAccess(
        storeId: StoreId,
        userId: String,
    ): PartnerStoreAccessSnapshot
}
