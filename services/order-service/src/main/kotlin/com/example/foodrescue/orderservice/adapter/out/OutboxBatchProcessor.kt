package com.example.foodrescue.orderservice.adapter.out

import com.example.foodrescue.orderservice.adapter.out.db.entities.OutboxEventJpaEntity
import com.example.foodrescue.orderservice.adapter.out.db.persistence.OutboxEventJpaRepository
import com.example.foodrescue.orderservice.configuration.OrderKafkaProperties
import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OutboxBatchProcessor(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val clock: Clock,
    private val kafkaProperties: OrderKafkaProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun publishBatch(
        batchSize: Int,
        maxAttempts: Int,
    ): Int {
        require(batchSize > 0) {
            "Outbox batchSize must be greater than zero"
        }
        require(maxAttempts > 0) {
            "Outbox maxAttempts must be greater than zero"
        }

        val events =
            outboxEventJpaRepository.lockPublishableBatch(
                batchSize = batchSize,
                maxAttempts = maxAttempts,
            )

        var publishedCount = 0

        for (event in events) {
            try {
                publish(event)
                event.markPublished(clock.instant())
                publishedCount += 1

                logger.info(
                    "Kafka event published successfully: eventId={}, eventType={}, aggregateId={}, topic={}",
                    event.id,
                    event.eventType,
                    event.aggregateId,
                    kafkaProperties.orderCommandsTopic,
                )
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                val failure = failureMessage(exception)
                event.markFailed(failure)

                logger.error(
                    "Kafka event publication failed: eventId={}, eventType={}, aggregateId={}, topic={}, reason={}",
                    event.id,
                    event.eventType,
                    event.aggregateId,
                    kafkaProperties.orderCommandsTopic,
                    failure,
                )
                break
            } catch (exception: Exception) {
                val failure = failureMessage(exception)
                event.markFailed(failure)

                logger.error(
                    "Kafka event publication failed: eventId={}, eventType={}, aggregateId={}, topic={}, reason={}",
                    event.id,
                    event.eventType,
                    event.aggregateId,
                    kafkaProperties.orderCommandsTopic,
                    failure,
                )
            }
        }

        outboxEventJpaRepository.flush()

        return publishedCount
    }

    private fun publish(event: OutboxEventJpaEntity) {
        kafkaTemplate
            .send(
                kafkaProperties.orderCommandsTopic,
                event.aggregateId.toString(),
                event.payload.toString(),
            )
            .get()
    }

    private fun failureMessage(exception: Exception): String {
        var current: Throwable = exception

        while (current.cause != null && current.cause !== current) {
            current = checkNotNull(current.cause)
        }

        return current.message?.takeIf(String::isNotBlank)?.take(2_000)
            ?: "Kafka publication failed"
    }
}
