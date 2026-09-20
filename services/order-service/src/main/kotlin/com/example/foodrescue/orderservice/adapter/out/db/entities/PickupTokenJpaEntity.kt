package com.example.foodrescue.orderservice.adapter.out.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "pickup_tokens")
class PickupTokenJpaEntity(
    @Id
    @Column(
        name = "order_id",
        nullable = false,
        updatable = false,
    )
    var orderId: UUID,
    @Column(
        name = "token_hash",
        nullable = false,
        length = 64,
    )
    var tokenHash: String,
    @Column(name = "used_at") var usedAt: Instant?,
    @Version
    @Column(
        name = "version",
        nullable = false,
    )
    var version: Long,
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
    )
    var createdAt: Instant,
    @Column(
        name = "updated_at",
        nullable = false,
    )
    var updatedAt: Instant,
)
