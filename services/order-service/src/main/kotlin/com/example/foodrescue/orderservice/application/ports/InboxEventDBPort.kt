package com.example.foodrescue.orderservice.application.ports

import java.time.Instant
import java.util.UUID

interface InboxEventDBPort {
    fun tryMarkProcessed(
        eventId: UUID,
        eventType: String,
        aggregateId: UUID,
        processedAt: Instant,
    ): Boolean
}
