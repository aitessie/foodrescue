package com.example.foodrescue.partnerservice.adapter.out.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "store_staff",
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "uk_store_staff_user_store",
                columnNames = ["user_id", "store_id"],
            )
        ],
)
class StoreStaffJpaEntity(
    @Id @Column(name = "id", nullable = false) var id: UUID,
    @Column(name = "user_id", nullable = false) var userId: String,
    @Column(name = "store_id", nullable = false) var storeId: UUID,
    @Column(name = "created_at", nullable = false) var createdAt: Instant,
)
