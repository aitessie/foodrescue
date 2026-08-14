package com.example.foodrescue.offerservice.domain.entities

import java.time.Instant
import java.util.UUID

data class ProcessedPartnerEvent(
    val eventId: UUID,
    val eventType: String,
    val aggregateId: UUID,
    val aggregateVersion: Long,
    val occurredAt: Instant,
    val processedAt: Instant,
)
