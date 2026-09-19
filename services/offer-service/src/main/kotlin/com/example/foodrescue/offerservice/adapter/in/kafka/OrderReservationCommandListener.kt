package com.example.foodrescue.offerservice.adapter.`in`.kafka

import com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos.OrderReservationCommandEnvelopeDto
import com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos.OrderReservationEventPayloadDto
import com.example.foodrescue.offerservice.application.usecases.CommitOfferReservationUseCase
import com.example.foodrescue.offerservice.application.usecases.ProcessOrderReservationCommandUseCase
import com.example.foodrescue.offerservice.application.usecases.ReleaseFoodBagReservationUseCase
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OrderReservationCommandListener(
    private val objectMapper: ObjectMapper,
    private val processOrderReservationCommandUseCase: ProcessOrderReservationCommandUseCase,
    private val commitOfferReservationUseCase: CommitOfferReservationUseCase,
    private val releaseFoodBagReservationUseCase: ReleaseFoodBagReservationUseCase,
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

        val payload = readPayload(envelope)

        when (envelope.eventType) {
            RESERVATION_REQUESTED_EVENT_TYPE ->
                processOrderReservationCommandUseCase.execute(
                    reservationId = ReservationId(payload.orderId),
                    offerId = OfferId(payload.offerId),
                    customerId = payload.customerId,
                    quantity = payload.quantity,
                )

            RESERVATION_COMMIT_REQUESTED_EVENT_TYPE ->
                commitOfferReservationUseCase.execute(
                    reservationId = ReservationId(payload.orderId),
                    offerId = OfferId(payload.offerId),
                )

            RESERVATION_RELEASE_REQUESTED_EVENT_TYPE ->
                releaseFoodBagReservationUseCase.executeForOrder(
                    reservationId = ReservationId(payload.orderId),
                    offerId = OfferId(payload.offerId),
                )

            else -> error("Unsupported order command eventType: ${envelope.eventType}")
        }
    }

    private fun readPayload(
        envelope: OrderReservationCommandEnvelopeDto
    ): OrderReservationEventPayloadDto {
        val payload =
            objectMapper.readValue(
                envelope.payload.toString(),
                OrderReservationEventPayloadDto::class.java,
            )

        require(envelope.aggregateId == payload.orderId) {
            "Order command aggregateId must match payload orderId"
        }
        require(payload.customerId.isNotBlank()) {
            "Order command customerId must not be blank"
        }
        require(payload.quantity > 0) {
            "Order command quantity must be greater than zero"
        }

        return payload
    }

    private fun validateEnvelope(envelope: OrderReservationCommandEnvelopeDto) {
        require(envelope.schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported order command schemaVersion: ${envelope.schemaVersion}"
        }
        require(envelope.aggregateVersion >= 0) {
            "Order command aggregateVersion must not be negative"
        }
    }

    private companion object {
        private const val RESERVATION_REQUESTED_EVENT_TYPE = "order.reservation-requested"
        private const val RESERVATION_COMMIT_REQUESTED_EVENT_TYPE =
            "order.reservation-commit-requested"
        private const val RESERVATION_RELEASE_REQUESTED_EVENT_TYPE =
            "order.reservation-release-requested"
        private const val SUPPORTED_SCHEMA_VERSION = 1
    }
}
