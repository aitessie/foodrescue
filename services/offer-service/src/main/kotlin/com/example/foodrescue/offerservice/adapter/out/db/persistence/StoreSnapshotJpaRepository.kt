package com.example.foodrescue.offerservice.adapter.`out`.db.persistence

import com.example.foodrescue.offerservice.adapter.`out`.db.entities.StoreSnapshotJpaEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StoreSnapshotJpaRepository : JpaRepository<StoreSnapshotJpaEntity, UUID> {
    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true,
    )
    @Query(
        value =
            """
            insert into store_snapshots (
                store_id,
                partner_id,
                partner_status,
                store_status,
                name,
                address,
                time_zone,
                store_version,
                partner_version
            )
            values (
                :storeId,
                :partnerId,
                :partnerStatus,
                :storeStatus,
                :name,
                :address,
                :timeZone,
                :storeVersion,
                0
            )
            on conflict (store_id) do update
            set
                store_status = excluded.store_status,
                name = excluded.name,
                address = excluded.address,
                time_zone = excluded.time_zone,
                store_version = excluded.store_version
            where store_snapshots.partner_id =
                    excluded.partner_id
              and excluded.store_version >
                    store_snapshots.store_version
            """,
        nativeQuery = true,
    )
    fun insertOrUpdateStoreSnapshotIfNewer(
        @Param("storeId") storeId: UUID,
        @Param("partnerId") partnerId: UUID,
        @Param("partnerStatus") partnerStatus: String,
        @Param("storeStatus") storeStatus: String,
        @Param("name") name: String,
        @Param("address") address: String,
        @Param("timeZone") timeZone: String,
        @Param("storeVersion") storeVersion: Long,
    )

    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true,
    )
    @Query(
        value =
            """
            update store_snapshots
            set
                partner_status = :partnerStatus,
                partner_version = :partnerVersion
            where partner_id = :partnerId
              and partner_version < :partnerVersion
            """,
        nativeQuery = true,
    )
    fun updatePartnerStatusIfNewer(
        @Param("partnerId") partnerId: UUID,
        @Param("partnerStatus") partnerStatus: String,
        @Param("partnerVersion") partnerVersion: Long,
    )
}
