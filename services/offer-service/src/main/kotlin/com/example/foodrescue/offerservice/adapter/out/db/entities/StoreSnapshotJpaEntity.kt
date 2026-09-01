package com.example.foodrescue.offerservice.adapter.`out`.db.entities

import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus
import com.example.foodrescue.offerservice.domain.`enum`.StoreStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "store_snapshots")
class StoreSnapshotJpaEntity(
    @Id
    @Column(
        name = "store_id",
        nullable = false,
        updatable = false,
    )
    var storeId: UUID,
    @Column(
        name = "partner_id",
        nullable = false,
        updatable = false,
    )
    var partnerId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(
        name = "partner_status",
        nullable = false,
        length = 32,
    )
    var partnerStatus: PartnerStatus,
    @Enumerated(EnumType.STRING)
    @Column(
        name = "store_status",
        nullable = false,
        length = 32,
    )
    var storeStatus: StoreStatus,
    @Column(
        name = "name",
        nullable = false,
        length = 255,
    )
    var name: String,
    @Column(
        name = "address",
        nullable = false,
        columnDefinition = "text",
    )
    var address: String,
    @Column(
        name = "time_zone",
        nullable = false,
        length = 64,
    )
    var timeZone: String,
    @Column(
        name = "store_version",
        nullable = false,
    )
    var storeVersion: Long,
    @Column(
        name = "partner_version",
        nullable = false,
    )
    var partnerVersion: Long,
)
