package com.example.foodrescue.offerservice.adapter.`out`.db.persistence

import com.example.foodrescue.offerservice.adapter.`out`.db.mappers.StoreSnapshotJpaMapper
import com.example.foodrescue.offerservice.application.ports.StoreSnapshotDBPort
import com.example.foodrescue.offerservice.domain.entities.PartnerStatusSnapshotUpdate
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.entities.StoreSnapshot
import com.example.foodrescue.offerservice.domain.entities.StoreSnapshotUpdate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class StoreSnapshotRepository(
    private val storeSnapshotJpaRepository: StoreSnapshotJpaRepository,
    private val storeSnapshotJpaMapper: StoreSnapshotJpaMapper,
) : StoreSnapshotDBPort {
    @Transactional(readOnly = true)
    override fun findById(storeId: StoreId): StoreSnapshot? =
        storeSnapshotJpaRepository
            .findById(storeId.value)
            .orElse(null)
            ?.let(storeSnapshotJpaMapper::toDomain)

    @Transactional
    override fun applyStoreSnapshot(update: StoreSnapshotUpdate) {
        storeSnapshotJpaRepository.insertOrUpdateStoreSnapshotIfNewer(
            storeId = update.storeId.value,
            partnerId = update.partnerId.value,
            partnerStatus = update.partnerStatus.code,
            storeStatus = update.storeStatus.code,
            name = update.name,
            address = update.address,
            timeZone = update.timeZone.id,
            storeVersion = update.storeVersion,
        )
    }

    @Transactional
    override fun applyPartnerStatus(update: PartnerStatusSnapshotUpdate) {
        storeSnapshotJpaRepository.updatePartnerStatusIfNewer(
            partnerId = update.partnerId.value,
            partnerStatus = update.partnerStatus.code,
            partnerVersion = update.partnerVersion,
        )
    }
}
