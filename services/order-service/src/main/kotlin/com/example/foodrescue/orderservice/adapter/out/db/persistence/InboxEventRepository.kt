package com.example.foodrescue.orderservice.adapter.out.db.persistence

import com.example.foodrescue.orderservice.application.ports.InboxEventDBPort
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class InboxEventRepository(
    private val inboxEventJpaRepository: InboxEventJpaRepository,
) : InboxEventDBPort {
    override fun tryMarkProcessed(
        eventId: UUID,
        eventType: String,
        aggregateId: UUID,
        processedAt: Instant,
    ): Boolean =
        inboxEventJpaRepository.insertIfAbsent(
            eventId = eventId,
            eventType = eventType,
            aggregateId = aggregateId,
            processedAt = processedAt,
        ) == 1
}
