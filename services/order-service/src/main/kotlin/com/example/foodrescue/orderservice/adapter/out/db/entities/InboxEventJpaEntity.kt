package com.example.foodrescue.orderservice.adapter.out.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "inbox_events")
class InboxEventJpaEntity(
    @Id
    @Column(
        name = "event_id",
        nullable = false,
    )
    var eventId: UUID,
    @Column(
        name = "event_type",
        nullable = false,
        length = 128,
    )
    var eventType: String,
    @Column(
        name = "aggregate_id",
        nullable = false,
    )
    var aggregateId: UUID,
    @Column(
        name = "processed_at",
        nullable = false,
    )
    var processedAt: Instant,
)
