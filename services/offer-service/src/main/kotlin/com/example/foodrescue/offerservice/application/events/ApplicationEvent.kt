package com.example.foodrescue.offerservice.application.events

import java.time.Instant
import java.util.UUID

data class ApplicationEvent<out T : ApplicationEventPayload>(
    val eventId: UUID,
    val eventType: String,
    val schemaVersion: Int,
    val aggregateId: UUID,
    val aggregateVersion: Long,
    val occurredAt: Instant,
    val payload: T,
) {
    init {
        require(eventType.isNotBlank()) {
            "Application event type must not be blank"
        }
        require(schemaVersion > 0) {
            "Application event schemaVersion must be greater than zero"
        }
        require(aggregateVersion >= 0) {
            "Application event aggregateVersion must not be negative"
        }
    }
}
