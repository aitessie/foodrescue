package com.example.foodrescue.offerservice.adapter.out

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxRelay(
    private val outboxBatchProcessor: OutboxBatchProcessor,
    @Value("\${food-rescue.outbox.batch-size:100}") private val batchSize: Int,
    @Value("\${food-rescue.outbox.max-attempts:10}") private val maxAttempts: Int,
) {
    @Scheduled(
        fixedDelayString = "\${food-rescue.outbox.fixed-delay:1s}",
        initialDelayString = "\${food-rescue.outbox.initial-delay:10s}",
    )
    fun relay() {
        outboxBatchProcessor.publishBatch(
            batchSize = batchSize,
            maxAttempts = maxAttempts,
        )
    }
}
