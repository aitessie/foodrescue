package com.example.foodrescue.offerservice.adapter.out

import com.example.foodrescue.offerservice.adapter.out.db.entities.OutboxEventJpaEntity
import com.example.foodrescue.offerservice.adapter.out.db.entities.OutboxEventStatus
import com.example.foodrescue.offerservice.adapter.out.db.persistence.OutboxEventJpaRepository
import com.example.foodrescue.offerservice.application.events.ApplicationEvent
import com.example.foodrescue.offerservice.application.events.ApplicationEventPayload
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Service
class OutboxDomainEventPublisherAdapterService(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val objectMapper: ObjectMapper,
) : DomainEventPublisherPort {
    @Transactional(propagation = Propagation.MANDATORY)
    override fun publish(event: ApplicationEvent<ApplicationEventPayload>) {
        val payload: JsonNode = objectMapper.valueToTree(event)

        outboxEventJpaRepository.save(
            OutboxEventJpaEntity(
                id = event.eventId,
                eventType = event.eventType,
                schemaVersion = event.schemaVersion,
                aggregateId = event.aggregateId,
                aggregateVersion = event.aggregateVersion,
                payload = payload,
                status = OutboxEventStatus.NEW,
                attempts = 0,
                occurredAt = event.occurredAt,
                publishedAt = null,
                lastError = null,
            )
        )
    }
}
