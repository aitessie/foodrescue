package com.example.foodrescue.offerservice.application.events

import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ApplicationEventFactory {
    fun foodBagCreated(
        foodBag: FoodBag,
        occurredAt: Instant,
    ): ApplicationEvent<FoodBagEventPayload> =
        createFoodBagEvent(
            eventType = ApplicationEventType.FOOD_BAG_CREATED,
            foodBag = foodBag,
            occurredAt = occurredAt,
        )

    fun foodBagUpdated(
        foodBag: FoodBag,
        occurredAt: Instant,
    ): ApplicationEvent<FoodBagEventPayload> =
        createFoodBagEvent(
            eventType = ApplicationEventType.FOOD_BAG_UPDATED,
            foodBag = foodBag,
            occurredAt = occurredAt,
        )

    fun foodBagStatusChanged(
        foodBag: FoodBag,
        occurredAt: Instant,
    ): ApplicationEvent<FoodBagEventPayload> =
        createFoodBagEvent(
            eventType = ApplicationEventType.FOOD_BAG_STATUS_CHANGED,
            foodBag = foodBag,
            occurredAt = occurredAt,
        )

    fun offerCreated(
        offer: Offer,
        occurredAt: Instant,
    ): ApplicationEvent<OfferEventPayload> =
        createOfferEvent(
            eventType = ApplicationEventType.OFFER_CREATED,
            offer = offer,
            occurredAt = occurredAt,
        )

    fun offerUpdated(
        offer: Offer,
        occurredAt: Instant,
    ): ApplicationEvent<OfferEventPayload> =
        createOfferEvent(
            eventType = ApplicationEventType.OFFER_UPDATED,
            offer = offer,
            occurredAt = occurredAt,
        )

    fun offerStatusChanged(
        offer: Offer,
        occurredAt: Instant,
    ): ApplicationEvent<OfferEventPayload> =
        createOfferEvent(
            eventType = ApplicationEventType.OFFER_STATUS_CHANGED,
            offer = offer,
            occurredAt = occurredAt,
        )

    fun offerQuantityChanged(
        offer: Offer,
        occurredAt: Instant,
    ): ApplicationEvent<OfferEventPayload> =
        createOfferEvent(
            eventType = ApplicationEventType.OFFER_QUANTITY_CHANGED,
            offer = offer,
            occurredAt = occurredAt,
        )

    fun offerReserved(
        offer: Offer,
        reservation: OfferReservation,
        occurredAt: Instant,
    ): ApplicationEvent<OfferReservationEventPayload> =
        createReservationEvent(
            eventType = ApplicationEventType.OFFER_RESERVED,
            offer = offer,
            reservation = reservation,
            occurredAt = occurredAt,
        )

    fun offerReservationReleased(
        offer: Offer,
        reservation: OfferReservation,
        occurredAt: Instant,
    ): ApplicationEvent<OfferReservationEventPayload> =
        createReservationEvent(
            eventType = ApplicationEventType.OFFER_RESERVATION_RELEASED,
            offer = offer,
            reservation = reservation,
            occurredAt = occurredAt,
        )

    fun offerClosed(
        offer: Offer,
        occurredAt: Instant,
    ): ApplicationEvent<OfferEventPayload> =
        createOfferEvent(
            eventType = ApplicationEventType.OFFER_CLOSED,
            offer = offer,
            occurredAt = occurredAt,
        )

    private fun createFoodBagEvent(
        eventType: ApplicationEventType,
        foodBag: FoodBag,
        occurredAt: Instant,
    ): ApplicationEvent<FoodBagEventPayload> =
        ApplicationEvent(
            eventId = UUID.randomUUID(),
            eventType = eventType.code,
            schemaVersion = APPLICATION_EVENT_SCHEMA_VERSION,
            aggregateId = foodBag.id.value,
            aggregateVersion = foodBag.version,
            occurredAt = occurredAt,
            payload =
                FoodBagEventPayload(
                    foodBagId = foodBag.id,
                    storeId = foodBag.storeId,
                    name = foodBag.name,
                    description = foodBag.description,
                    category = foodBag.category,
                    originalPrice = foodBag.originalPrice,
                    unitPrice = foodBag.unitPrice,
                    allergens = foodBag.allergens,
                    status = foodBag.status,
                    createdAt = foodBag.createdAt,
                    updatedAt = foodBag.updatedAt,
                ),
        )

    private fun createOfferEvent(
        eventType: ApplicationEventType,
        offer: Offer,
        occurredAt: Instant,
    ): ApplicationEvent<OfferEventPayload> =
        ApplicationEvent(
            eventId = UUID.randomUUID(),
            eventType = eventType.code,
            schemaVersion = APPLICATION_EVENT_SCHEMA_VERSION,
            aggregateId = offer.id.value,
            aggregateVersion = offer.version,
            occurredAt = occurredAt,
            payload =
                OfferEventPayload(
                    offerId = offer.id,
                    storeId = offer.storeId,
                    foodBagId = offer.foodBagId,
                    category = offer.category,
                    unitPrice = offer.unitPrice,
                    allergens = offer.allergens,
                    status = offer.status,
                    totalQuantity = offer.totalQuantity,
                    availableQuantity = offer.availableQuantity,
                    pickupWindow = offer.pickupWindow,
                    createdAt = offer.createdAt,
                    updatedAt = offer.updatedAt,
                ),
        )

    private fun createReservationEvent(
        eventType: ApplicationEventType,
        offer: Offer,
        reservation: OfferReservation,
        occurredAt: Instant,
    ): ApplicationEvent<OfferReservationEventPayload> =
        ApplicationEvent(
            eventId = UUID.randomUUID(),
            eventType = eventType.code,
            schemaVersion = APPLICATION_EVENT_SCHEMA_VERSION,
            aggregateId = offer.id.value,
            aggregateVersion = offer.version,
            occurredAt = occurredAt,
            payload =
                OfferReservationEventPayload(
                    reservationId = reservation.id,
                    offerId = offer.id,
                    quantity = reservation.quantity,
                    reservationStatus = reservation.status,
                    offerTotalQuantity = offer.totalQuantity,
                    offerAvailableQuantity = offer.availableQuantity,
                    offerReservedQuantity = offer.reservedQuantity,
                    reservationCreatedAt = reservation.createdAt,
                    reservationUpdatedAt = reservation.updatedAt,
                ),
        )

    private companion object {
        private const val APPLICATION_EVENT_SCHEMA_VERSION = 1
    }
}
