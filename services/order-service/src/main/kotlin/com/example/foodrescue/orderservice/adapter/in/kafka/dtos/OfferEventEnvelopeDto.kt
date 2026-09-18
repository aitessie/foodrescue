package com.example.foodrescue.orderservice.adapter.`in`.kafka.dtos

import java.time.Instant
import java.util.UUID
import tools.jackson.databind.JsonNode

data class OfferEventEnvelopeDto(
    val eventId: UUID,
    val eventType: String,
    val schemaVersion: Int,
    val aggregateId: UUID,
    val aggregateVersion: Long,
    val occurredAt: Instant,
    val payload: JsonNode,
)
