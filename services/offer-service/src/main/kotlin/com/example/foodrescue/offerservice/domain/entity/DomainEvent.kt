package com.example.foodrescue.offerservice.domain.entity

import java.time.Instant
import java.util.UUID

class DomainEvent(
    val eventId: UUID,
    eventType: String,
    schemaVersion: Int,
    val aggregateId: UUID,
    aggregateVersion: Long,
    val occurredAt: Instant,
    payload: Map<String, Any?>,
) {
    val eventType: String = validateEventType(eventType)

    val schemaVersion: Int = validateSchemaVersion(schemaVersion)

    val aggregateVersion: Long = validateAggregateVersion(aggregateVersion)

    val payload: Map<String, Any?> = payload.toMap()
}

private fun validateEventType(value: String): String {
    require(value.isNotBlank()) {
        "Domain event type must not be blank"
    }

    return value
}

private fun validateSchemaVersion(value: Int): Int {
    require(value > 0) {
        "Domain event schemaVersion must be greater than zero"
    }

    return value
}

private fun validateAggregateVersion(value: Long): Long {
    require(value >= 0) {
        "Domain event aggregateVersion must not be negative"
    }

    return value
}
