package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.exceptions.OrderValidationException
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MarkNoShowOrdersUseCase(
    private val orderDBPort: OrderDBPort,
    private val processor: MarkNoShowOrderProcessor,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(batchSize: Int): Int {
        validateBatchSize(batchSize)

        val now = clock.instant()
        val candidates =
            orderDBPort.findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = batchSize,
            )

        var markedOrders = 0

        candidates.forEach { order ->
            try {
                if (
                    processor.markIfExpired(
                        orderId = order.id,
                        now = now,
                    )
                ) {
                    markedOrders += 1
                }
            } catch (exception: Exception) {
                logger.error(
                    "Failed to mark Order as no-show: orderId={}",
                    order.id.value,
                    exception,
                )
            }
        }

        return markedOrders
    }

    private fun validateBatchSize(batchSize: Int) {
        if (batchSize <= 0) {
            throw OrderValidationException("No-show batchSize must be greater than zero")
        }
    }
}
