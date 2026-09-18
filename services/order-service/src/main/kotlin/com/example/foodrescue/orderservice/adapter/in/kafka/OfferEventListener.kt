package com.example.foodrescue.orderservice.adapter.`in`.kafka

import com.example.foodrescue.orderservice.adapter.`in`.kafka.dtos.OfferEventEnvelopeDto
import com.example.foodrescue.orderservice.adapter.`in`.kafka.dtos.OfferReservationRejectedEventPayloadDto
import com.example.foodrescue.orderservice.adapter.`in`.kafka.dtos.OfferReservedEventPayloadDto
import com.example.foodrescue.orderservice.application.events.OfferReservationResult
import com.example.foodrescue.orderservice.application.usecases.ProcessOfferReservationResultUseCase
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.OrderId
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OfferEventListener(
    private val objectMapper: ObjectMapper,
    private val processOfferReservationResultUseCase: ProcessOfferReservationResultUseCase,
) {
    @KafkaListener(
        topics = ["\${food-rescue.kafka.offer-events-topic}"],
        groupId = "\${food-rescue.kafka.offer-events-consumer-group}",
        containerFactory = "offerEventKafkaListenerContainerFactory",
    )
    fun onMessage(message: String) {
        val envelope = objectMapper.readValue(message, OfferEventEnvelopeDto::class.java)

        when (envelope.eventType) {
            OFFER_RESERVED_EVENT_TYPE -> processReserved(envelope)
            OFFER_RESERVATION_REJECTED_EVENT_TYPE -> processRejected(envelope)
        }
    }

    private fun processReserved(envelope: OfferEventEnvelopeDto) {
        validateEnvelope(envelope)

        val payload =
            objectMapper.readValue(
                envelope.payload.toString(),
                OfferReservedEventPayloadDto::class.java,
            )

        require(payload.reservationStatus == RESERVED_STATUS) {
            "Unsupported reservation status: ${payload.reservationStatus}"
        }
        require(payload.quantity > 0) {
            "Offer reservation quantity must be greater than zero"
        }
        require(envelope.aggregateId == payload.offerId) {
            "Offer reserved aggregateId must match payload offerId"
        }

        processOfferReservationResultUseCase.execute(
            eventId = envelope.eventId,
            eventType = envelope.eventType,
            aggregateId = envelope.aggregateId,
            orderId = OrderId(payload.reservationId),
            offerId = OfferId(payload.offerId),
            quantity = payload.quantity,
            result = OfferReservationResult.HELD,
        )
    }

    private fun processRejected(envelope: OfferEventEnvelopeDto) {
        validateEnvelope(envelope)

        val payload =
            objectMapper.readValue(
                envelope.payload.toString(),
                OfferReservationRejectedEventPayloadDto::class.java,
            )

        require(payload.customerId.isNotBlank()) {
            "Offer reservation rejection customerId must not be blank"
        }
        require(payload.quantity > 0) {
            "Offer reservation rejection quantity must be greater than zero"
        }
        require(payload.reason.isNotBlank()) {
            "Offer reservation rejection reason must not be blank"
        }
        require(envelope.aggregateId == payload.reservationId) {
            "Offer reservation rejection aggregateId must match payload reservationId"
        }

        processOfferReservationResultUseCase.execute(
            eventId = envelope.eventId,
            eventType = envelope.eventType,
            aggregateId = envelope.aggregateId,
            orderId = OrderId(payload.reservationId),
            offerId = OfferId(payload.offerId),
            quantity = payload.quantity,
            result = OfferReservationResult.REJECTED,
        )
    }

    private fun validateEnvelope(envelope: OfferEventEnvelopeDto) {
        require(envelope.schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported Offer event schemaVersion: ${envelope.schemaVersion}"
        }
        require(envelope.aggregateVersion >= 0) {
            "Offer event aggregateVersion must not be negative"
        }
    }

    private companion object {
        private const val OFFER_RESERVED_EVENT_TYPE = "offer.reserved"
        private const val OFFER_RESERVATION_REJECTED_EVENT_TYPE = "offer.reservation-rejected"
        private const val RESERVED_STATUS = "RESERVED"
        private const val SUPPORTED_SCHEMA_VERSION = 1
    }
}
