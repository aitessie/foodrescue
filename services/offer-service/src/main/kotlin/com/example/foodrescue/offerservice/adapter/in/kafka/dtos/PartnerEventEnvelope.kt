package com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos

import java.time.Instant
import java.util.UUID
import tools.jackson.databind.JsonNode

data class PartnerEventEnvelope(
    val eventId: UUID,
    val eventType: String,
    val schemaVersion: Int,
    val aggregateId: UUID,
    val aggregateVersion: Long,
    val occurredAt: Instant,
    val payload: JsonNode,
)
