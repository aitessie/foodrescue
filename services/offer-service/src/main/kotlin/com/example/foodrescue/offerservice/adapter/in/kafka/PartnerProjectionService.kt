package com.example.foodrescue.offerservice.adapter.`in`.kafka

import com.example.foodrescue.offerservice.application.ports.StoreSnapshotDBPort
import com.example.foodrescue.offerservice.domain.entities.PartnerStatusSnapshotUpdate
import com.example.foodrescue.offerservice.domain.entities.StoreSnapshotUpdate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PartnerProjectionService(private val storeSnapshotDBPort: StoreSnapshotDBPort) {
    @Transactional
    fun applyStoreSnapshot(update: StoreSnapshotUpdate) {
        storeSnapshotDBPort.applyStoreSnapshot(update)
    }

    @Transactional
    fun applyPartnerStatus(update: PartnerStatusSnapshotUpdate) {
        storeSnapshotDBPort.applyPartnerStatus(update)
    }
}
