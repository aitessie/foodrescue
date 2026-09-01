package com.example.foodrescue.offerservice.adapter.`out`.db.entities

import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
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
@Table(name = "offer_reservations")
class OfferReservationJpaEntity(
    @Id
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
    )
    var id: UUID,
    @Column(
        name = "offer_id",
        nullable = false,
        updatable = false,
    )
    var offerId: UUID,
    @Column(
        name = "customer_id",
        nullable = false,
        updatable = false,
        length = 255,
    )
    var customerId: String,
    @Column(
        name = "quantity",
        nullable = false,
        updatable = false,
    )
    var quantity: Int,
    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 32,
    )
    var status: ReservationStatus,
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
    @Version
    @Column(
        name = "version",
        nullable = false,
    )
    var version: Long,
)
