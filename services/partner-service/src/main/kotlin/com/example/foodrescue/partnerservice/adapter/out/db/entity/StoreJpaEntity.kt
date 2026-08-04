package com.example.foodrescue.partnerservice.adapter.out.db.entity

import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import java.time.Instant

@Entity
@Table(name = "stores")
class StoreJpaEntity(

    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "partner_id", nullable = false)
    var partnerId: UUID,

    @Column(name = "name", nullable = false, length = 200)
    var name: String,

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var status: StoreStatus,

    @Embedded
    var address: AddressJpaEmbeddable,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "store_working_hours",
        joinColumns = [JoinColumn(name = "store_id")]
    )
    @OrderColumn(name = "position")
    var workingHours: MutableList<WorkingHoursJpaEmbeddable>,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
)
