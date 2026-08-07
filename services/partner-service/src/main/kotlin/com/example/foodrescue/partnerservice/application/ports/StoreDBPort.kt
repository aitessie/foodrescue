package com.example.foodrescue.partnerservice.application.ports

import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import com.example.foodrescue.partnerservice.domain.entity.Store
import com.example.foodrescue.partnerservice.domain.entity.StoreId

interface StoreDBPort {
    fun save(store: Store): Store
    fun findById(storeId: StoreId): Store?
    fun findAllByPartnerId(partnerId: PartnerId): List<Store>
}
