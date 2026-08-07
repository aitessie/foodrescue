package com.example.foodrescue.partnerservice.adapter.out.db.entity

import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "partners")
class PartnerJpaEntity(
    @Id @Column(name = "id", nullable = false) var id: UUID,
    @Column(name = "manager_id", nullable = false) var managerId: String,
    @Column(name = "name", nullable = false) var name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PartnerStatus,
    @Column(name = "created_at", nullable = false) var createdAt: Instant,
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant,
    @Version @Column(name = "version", nullable = false) var version: Long = 0,
)
