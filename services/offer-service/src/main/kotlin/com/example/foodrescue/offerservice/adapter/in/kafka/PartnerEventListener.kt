package com.example.foodrescue.offerservice.adapter.`in`.kafka

import com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos.PartnerEventEnvelope
import com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos.PartnerEventPayloadV1
import com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos.StoreEventPayloadV1
import com.example.foodrescue.offerservice.adapter.`in`.mappers.PartnerEventMapper
import com.example.foodrescue.offerservice.application.exceptions.InvalidPartnerEventException
import com.example.foodrescue.offerservice.application.exceptions.UnsupportedPartnerEventSchemaException
import com.example.foodrescue.offerservice.application.exceptions.UnsupportedPartnerEventTypeException
import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus
import com.example.foodrescue.offerservice.domain.`enum`.StoreStatus
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PartnerEventListener(
    private val objectMapper: ObjectMapper,
    private val eventMapper: PartnerEventMapper,
    private val projectionService: PartnerProjectionService,
) {
    @KafkaListener(
        topics = ["\${food-rescue.kafka.partner-events-topic}"],
        containerFactory = "partnerEventKafkaListenerContainerFactory",
    )
    fun listen(message: String) {
        val envelope =
            objectMapper.readValue(
                message,
                PartnerEventEnvelope::class.java,
            )

        validateSchemaVersion(envelope)

        val eventType = resolveEventType(envelope.eventType)

        when (eventType) {
            PartnerEventType.STORE_CREATED,
            PartnerEventType.STORE_UPDATED,
            PartnerEventType.STORE_SUSPENDED ->
                applyStoreEvent(
                    envelope = envelope,
                    eventType = eventType,
                )

            PartnerEventType.PARTNER_UPDATED,
            PartnerEventType.PARTNER_SUSPENDED ->
                applyPartnerEvent(
                    envelope = envelope,
                    eventType = eventType,
                )
        }
    }

    private fun applyStoreEvent(
        envelope: PartnerEventEnvelope,
        eventType: PartnerEventType,
    ) {
        val payload =
            objectMapper.treeToValue(
                envelope.payload,
                StoreEventPayloadV1::class.java,
            )

        val update =
            eventMapper.toStoreSnapshot(
                envelope = envelope,
                payload = payload,
            )

        validateStoreEvent(
            envelope = envelope,
            eventType = eventType,
            payload = payload,
            storeStatus = update.storeStatus,
        )

        projectionService.applyStoreSnapshot(update)
    }

    private fun applyPartnerEvent(
        envelope: PartnerEventEnvelope,
        eventType: PartnerEventType,
    ) {
        val payload =
            objectMapper.treeToValue(
                envelope.payload,
                PartnerEventPayloadV1::class.java,
            )

        val update =
            eventMapper.toPartnerStatusUpdate(
                envelope = envelope,
                payload = payload,
            )

        validatePartnerEvent(
            envelope = envelope,
            eventType = eventType,
            payload = payload,
            partnerStatus = update.partnerStatus,
        )

        projectionService.applyPartnerStatus(update)
    }

    private fun resolveEventType(eventTypeCode: String): PartnerEventType =
        PartnerEventType.entries.firstOrNull { eventType ->
            eventType.code == eventTypeCode
        } ?: throw UnsupportedPartnerEventTypeException(eventTypeCode)

    private fun validateSchemaVersion(envelope: PartnerEventEnvelope) {
        if (envelope.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw UnsupportedPartnerEventSchemaException(envelope.schemaVersion)
        }
    }

    private fun validateStoreEvent(
        envelope: PartnerEventEnvelope,
        eventType: PartnerEventType,
        payload: StoreEventPayloadV1,
        storeStatus: StoreStatus,
    ) {
        if (envelope.aggregateId != payload.storeId) {
            throw InvalidPartnerEventException("Partner event aggregateId does not match storeId")
        }

        if (eventType == PartnerEventType.STORE_SUSPENDED && storeStatus != StoreStatus.SUSPENDED) {
            throw InvalidPartnerEventException(
                "store.suspended event must contain SUSPENDED storeStatus"
            )
        }
    }

    private fun validatePartnerEvent(
        envelope: PartnerEventEnvelope,
        eventType: PartnerEventType,
        payload: PartnerEventPayloadV1,
        partnerStatus: PartnerStatus,
    ) {
        if (envelope.aggregateId != payload.partnerId) {
            throw InvalidPartnerEventException("Partner event aggregateId does not match partnerId")
        }

        if (
            eventType == PartnerEventType.PARTNER_SUSPENDED &&
                partnerStatus != PartnerStatus.SUSPENDED
        ) {
            throw InvalidPartnerEventException(
                "partner.suspended event must contain SUSPENDED partnerStatus"
            )
        }
    }

    companion object {
        private const val SUPPORTED_SCHEMA_VERSION = 1
    }
}
