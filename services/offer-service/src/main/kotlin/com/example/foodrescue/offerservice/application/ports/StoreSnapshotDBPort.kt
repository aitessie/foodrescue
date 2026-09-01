package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entities.PartnerStatusSnapshotUpdate
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.entities.StoreSnapshot
import com.example.foodrescue.offerservice.domain.entities.StoreSnapshotUpdate

interface StoreSnapshotDBPort {
    fun findById(storeId: StoreId): StoreSnapshot?

    fun applyStoreSnapshot(update: StoreSnapshotUpdate)

    fun applyPartnerStatus(update: PartnerStatusSnapshotUpdate)
}
