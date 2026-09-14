package com.example.foodrescue.orderservice.adapter.out.db.entities

import com.example.foodrescue.orderservice.domain.enum.OrderStatus
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
@Table(name = "orders")
class OrderJpaEntity(
    @Id
    @Column(
        name = "id",
        nullable = false,
    )
    var id: UUID,
    @Column(
        name = "customer_id",
        nullable = false,
        length = 255,
    )
    var customerId: String,
    @Column(
        name = "offer_id",
        nullable = false,
    )
    var offerId: UUID,
    @Column(
        name = "store_id",
        nullable = false,
    )
    var storeId: UUID,
    @Column(
        name = "quantity",
        nullable = false,
    )
    var quantity: Int,
    @Column(
        name = "unit_price",
        nullable = false,
    )
    var unitPrice: Long,
    @Column(
        name = "total_amount",
        nullable = false,
    )
    var totalAmount: Long,
    @Column(
        name = "pickup_start",
        nullable = false,
    )
    var pickupStart: Instant,
    @Column(
        name = "pickup_end",
        nullable = false,
    )
    var pickupEnd: Instant,
    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 64,
    )
    var status: OrderStatus,
    @Version
    @Column(
        name = "version",
        nullable = false,
    )
    var version: Long,
    @Column(
        name = "created_at",
        nullable = false,
    )
    var createdAt: Instant,
    @Column(
        name = "updated_at",
        nullable = false,
    )
    var updatedAt: Instant,
)
