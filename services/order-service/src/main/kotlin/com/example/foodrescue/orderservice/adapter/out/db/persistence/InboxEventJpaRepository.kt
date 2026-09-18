package com.example.foodrescue.orderservice.adapter.out.db.persistence

import com.example.foodrescue.orderservice.adapter.out.db.entities.InboxEventJpaEntity
import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

interface InboxEventJpaRepository : Repository<InboxEventJpaEntity, UUID> {
    @Modifying
    @Query(
        value =
            """
            INSERT INTO inbox_events (
                event_id,
                event_type,
                aggregate_id,
                processed_at
            )
            VALUES (
                :eventId,
                :eventType,
                :aggregateId,
                :processedAt
            )
            ON CONFLICT (event_id) DO NOTHING
            """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("eventId") eventId: UUID,
        @Param("eventType") eventType: String,
        @Param("aggregateId") aggregateId: UUID,
        @Param("processedAt") processedAt: Instant,
    ): Int
}
