package com.example.foodrescue.partnerservice.application.ports

import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.entities.StoreId

interface StoreDBPort {
    fun save(store: Store): Store

    fun findById(storeId: StoreId): Store?

    fun findAllByPartnerId(partnerId: PartnerId): List<Store>
}
