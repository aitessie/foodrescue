package com.example.foodrescue.partnerservice.application.events

import com.example.foodrescue.partnerservice.domain.entities.Address
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.Store
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ApplicationEventFactory {
    fun storeCreated(
        store: Store,
        partner: Partner,
        occurredAt: Instant,
    ): ApplicationEvent<StoreEventPayload> =
        createStoreEvent(
            eventType = ApplicationEventType.STORE_CREATED,
            store = store,
            partner = partner,
            occurredAt = occurredAt,
        )

    fun storeUpdated(
        store: Store,
        partner: Partner,
        occurredAt: Instant,
    ): ApplicationEvent<StoreEventPayload> =
        createStoreEvent(
            eventType = ApplicationEventType.STORE_UPDATED,
            store = store,
            partner = partner,
            occurredAt = occurredAt,
        )

    fun storeSuspended(
        store: Store,
        partner: Partner,
        occurredAt: Instant,
    ): ApplicationEvent<StoreEventPayload> =
        createStoreEvent(
            eventType = ApplicationEventType.STORE_SUSPENDED,
            store = store,
            partner = partner,
            occurredAt = occurredAt,
        )

    fun partnerUpdated(
        partner: Partner,
        occurredAt: Instant,
    ): ApplicationEvent<PartnerEventPayload> =
        createPartnerEvent(
            eventType = ApplicationEventType.PARTNER_UPDATED,
            partner = partner,
            occurredAt = occurredAt,
        )

    fun partnerSuspended(
        partner: Partner,
        occurredAt: Instant,
    ): ApplicationEvent<PartnerEventPayload> =
        createPartnerEvent(
            eventType = ApplicationEventType.PARTNER_SUSPENDED,
            partner = partner,
            occurredAt = occurredAt,
        )

    private fun createStoreEvent(
        eventType: ApplicationEventType,
        store: Store,
        partner: Partner,
        occurredAt: Instant,
    ): ApplicationEvent<StoreEventPayload> =
        ApplicationEvent(
            eventId = UUID.randomUUID(),
            eventType = eventType.code,
            schemaVersion = APPLICATION_EVENT_SCHEMA_VERSION,
            aggregateId = store.id.value,
            aggregateVersion = store.version,
            occurredAt = occurredAt,
            payload =
                StoreEventPayload(
                    storeId = store.id.value,
                    partnerId = store.partnerId.value,
                    partnerStatus = partner.status.code,
                    storeStatus = store.status.code,
                    name = store.name,
                    address = formatAddress(store.address),
                ),
        )

    private fun createPartnerEvent(
        eventType: ApplicationEventType,
        partner: Partner,
        occurredAt: Instant,
    ): ApplicationEvent<PartnerEventPayload> =
        ApplicationEvent(
            eventId = UUID.randomUUID(),
            eventType = eventType.code,
            schemaVersion = APPLICATION_EVENT_SCHEMA_VERSION,
            aggregateId = partner.id.value,
            aggregateVersion = partner.version,
            occurredAt = occurredAt,
            payload =
                PartnerEventPayload(
                    partnerId = partner.id.value,
                    partnerStatus = partner.status.code,
                ),
        )

    private fun formatAddress(address: Address): String =
        listOfNotNull(
                address.city,
                address.street,
                address.building,
                address.postalCode?.takeIf(String::isNotBlank),
            )
            .joinToString(", ")

    private companion object {
        private const val APPLICATION_EVENT_SCHEMA_VERSION = 1
    }
}
