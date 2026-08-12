package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entity.PartnerStatusProjectionUpdate
import com.example.foodrescue.offerservice.domain.entity.ProcessedPartnerEvent
import com.example.foodrescue.offerservice.domain.entity.StoreId
import com.example.foodrescue.offerservice.domain.entity.StoreProjectionUpdate
import com.example.foodrescue.offerservice.domain.entity.StoreReadModel

interface StoreReadModelDBPort {
    fun findById(storeId: StoreId): StoreReadModel?

    fun upsertStoreIfNewer(update: StoreProjectionUpdate): Boolean

    fun updatePartnerStatusIfNewer(update: PartnerStatusProjectionUpdate): Int

    fun tryMarkEventProcessed(event: ProcessedPartnerEvent): Boolean
}
