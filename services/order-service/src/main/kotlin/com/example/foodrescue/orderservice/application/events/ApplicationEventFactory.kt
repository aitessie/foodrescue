package com.example.foodrescue.orderservice.application.events

import com.example.foodrescue.orderservice.domain.entities.Order
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ApplicationEventFactory {
    fun orderReservationRequested(
        order: Order,
        occurredAt: Instant,
    ): ApplicationEvent<OrderReservationRequestedEventPayload> =
        ApplicationEvent(
            eventId = UUID.randomUUID(),
            eventType = ApplicationEventType.ORDER_RESERVATION_REQUESTED.code,
            schemaVersion = APPLICATION_EVENT_SCHEMA_VERSION,
            aggregateId = order.id.value,
            aggregateVersion = order.version,
            occurredAt = occurredAt,
            payload =
                OrderReservationRequestedEventPayload(
                    orderId = order.id,
                    offerId = order.offerId,
                    customerId = order.customerId,
                    quantity = order.quantity,
                ),
        )

    private companion object {
        private const val APPLICATION_EVENT_SCHEMA_VERSION = 1
    }
}
