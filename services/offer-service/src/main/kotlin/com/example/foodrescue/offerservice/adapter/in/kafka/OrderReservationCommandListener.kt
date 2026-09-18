package com.example.foodrescue.offerservice.adapter.`in`.kafka

import com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos.OrderReservationCommandEnvelopeDto
import com.example.foodrescue.offerservice.application.usecases.ProcessOrderReservationCommandUseCase
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OrderReservationCommandListener(
    private val objectMapper: ObjectMapper,
    private val processOrderReservationCommandUseCase: ProcessOrderReservationCommandUseCase,
) {
    @KafkaListener(
        topics = ["\${food-rescue.kafka.order-commands-topic}"],
        groupId = "\${food-rescue.kafka.order-commands-consumer-group}",
        containerFactory = "orderCommandKafkaListenerContainerFactory",
    )
    fun onMessage(message: String) {
        val envelope =
            objectMapper.readValue(message, OrderReservationCommandEnvelopeDto::class.java)
        validateEnvelope(envelope)

        processOrderReservationCommandUseCase.execute(
            reservationId = ReservationId(envelope.payload.orderId),
            offerId = OfferId(envelope.payload.offerId),
            customerId = envelope.payload.customerId,
            quantity = envelope.payload.quantity,
        )
    }

    private fun validateEnvelope(envelope: OrderReservationCommandEnvelopeDto) {
        require(envelope.eventType == RESERVATION_REQUESTED_EVENT_TYPE) {
            "Unsupported order command eventType: ${envelope.eventType}"
        }
        require(envelope.schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported order command schemaVersion: ${envelope.schemaVersion}"
        }
        require(envelope.aggregateVersion >= 0) {
            "Order command aggregateVersion must not be negative"
        }
        require(envelope.aggregateId == envelope.payload.orderId) {
            "Order command aggregateId must match payload orderId"
        }
        require(envelope.payload.customerId.isNotBlank()) {
            "Order command customerId must not be blank"
        }
        require(envelope.payload.quantity > 0) {
            "Order command quantity must be greater than zero"
        }
    }

    private companion object {
        private const val RESERVATION_REQUESTED_EVENT_TYPE = "order.reservation-requested"
        private const val SUPPORTED_SCHEMA_VERSION = 1
    }
}
