package com.example.foodrescue.offerservice.adapter.out.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tools.jackson.databind.JsonNode

@Entity
@Table(name = "outbox_events")
class OutboxEventJpaEntity(
    @field:Id
    @field:Column(
        name = "id",
        nullable = false,
        updatable = false,
        columnDefinition = "uuid",
    )
    var id: UUID,
    @field:Column(
        name = "event_type",
        nullable = false,
        updatable = false,
        length = 128,
    )
    var eventType: String,
    @field:Column(
        name = "schema_version",
        nullable = false,
        updatable = false,
    )
    var schemaVersion: Int,
    @field:Column(
        name = "aggregate_id",
        nullable = false,
        updatable = false,
        columnDefinition = "uuid",
    )
    var aggregateId: UUID,
    @field:Column(
        name = "aggregate_version",
        nullable = false,
        updatable = false,
    )
    var aggregateVersion: Long,
    @field:JdbcTypeCode(SqlTypes.JSON)
    @field:Column(
        name = "payload",
        nullable = false,
        updatable = false,
        columnDefinition = "jsonb",
    )
    var payload: JsonNode,
    @field:Enumerated(EnumType.STRING)
    @field:Column(
        name = "status",
        nullable = false,
        length = 32,
    )
    var status: OutboxEventStatus = OutboxEventStatus.NEW,
    @field:Column(
        name = "attempts",
        nullable = false,
    )
    var attempts: Int = 0,
    @field:Column(
        name = "occurred_at",
        nullable = false,
        updatable = false,
        columnDefinition = "timestamptz",
    )
    var occurredAt: Instant,
    @field:Column(
        name = "published_at",
        columnDefinition = "timestamptz",
    )
    var publishedAt: Instant? = null,
    @field:Column(
        name = "last_error",
        length = 2_000,
    )
    var lastError: String? = null,
) {
    fun markPublished(publishedAt: Instant) {
        status = OutboxEventStatus.PUBLISHED
        attempts += 1
        this.publishedAt = publishedAt
        lastError = null
    }

    fun markFailed(error: String) {
        status = OutboxEventStatus.FAILED
        attempts += 1
        publishedAt = null
        lastError = error.take(2_000)
    }
}
