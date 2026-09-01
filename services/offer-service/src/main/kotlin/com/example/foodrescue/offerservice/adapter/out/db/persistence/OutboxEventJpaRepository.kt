package com.example.foodrescue.offerservice.adapter.out.db.persistence

import com.example.foodrescue.offerservice.adapter.out.db.entities.OutboxEventJpaEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OutboxEventJpaRepository : JpaRepository<OutboxEventJpaEntity, UUID> {
    @Query(
        value =
            """
            SELECT outbox.*
            FROM outbox_events outbox
            WHERE outbox.status IN ('NEW', 'FAILED')
              AND outbox.attempts < :maxAttempts
            ORDER BY outbox.occurred_at ASC, outbox.id ASC
            LIMIT :batchSize
            FOR UPDATE
            """,
        nativeQuery = true,
    )
    fun lockPublishableBatch(
        @Param("batchSize") batchSize: Int,
        @Param("maxAttempts") maxAttempts: Int,
    ): List<OutboxEventJpaEntity>
}
