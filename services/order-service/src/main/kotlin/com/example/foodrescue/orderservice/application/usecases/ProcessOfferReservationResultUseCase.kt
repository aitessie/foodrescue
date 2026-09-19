package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.events.OfferReservationResult
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.ports.InboxEventDBPort
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PaymentCommandPort
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Clock
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProcessOfferReservationResultUseCase(
    private val orderDBPort: OrderDBPort,
    private val inboxEventDBPort: InboxEventDBPort,
    private val paymentCommandPort: PaymentCommandPort,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(
        eventId: UUID,
        eventType: String,
        aggregateId: UUID,
        orderId: OrderId,
        offerId: OfferId,
        quantity: Int,
        result: OfferReservationResult,
    ) {
        logger.info(
            "Trying to process Offer reservation result: eventId={}, orderId={}, result={}",
            eventId,
            orderId.value,
            result,
        )

        val now = clock.instant()
        val firstProcessing =
            inboxEventDBPort.tryMarkProcessed(
                eventId = eventId,
                eventType = eventType,
                aggregateId = aggregateId,
                processedAt = now,
            )

        if (!firstProcessing) {
            logger.info(
                "Offer reservation result already processed: eventId={}, orderId={}",
                eventId,
                orderId.value,
            )
            return
        }

        val order = orderDBPort.findById(orderId) ?: throw OrderNotFoundException(orderId)

        validateOrder(
            actualOfferId = order.offerId,
            actualQuantity = order.quantity,
            eventOfferId = offerId,
            eventQuantity = quantity,
        )

        when (result) {
            OfferReservationResult.HELD -> {
                if (order.status != OrderStatus.PENDING) {
                    throw OrderConflictException(
                        "Offer reservation cannot be held for Order in status ${order.status}"
                    )
                }

                paymentCommandPort.requestAuthorization(
                    orderId = order.id,
                    amount = order.totalAmount,
                )
            }

            OfferReservationResult.REJECTED -> {
                if (order.status == OrderStatus.FAILED) {
                    logger.info(
                        "Offer reservation rejection already reflected in Order: orderId={}, status={}",
                        order.id.value,
                        order.status,
                    )
                    return
                }

                if (order.status != OrderStatus.PENDING) {
                    throw OrderConflictException(
                        "Offer reservation cannot be rejected for Order in status ${order.status}"
                    )
                }

                order.status = OrderStatus.FAILED
                order.updatedAt = now
                orderDBPort.save(order)
            }
        }

        logger.info(
            "Offer reservation result processed successfully: eventId={}, orderId={}, result={}",
            eventId,
            order.id.value,
            result,
        )
    }

    private fun validateOrder(
        actualOfferId: OfferId,
        actualQuantity: Int,
        eventOfferId: OfferId,
        eventQuantity: Int,
    ) {
        if (actualOfferId != eventOfferId) {
            throw OrderConflictException("Offer reservation result belongs to another Offer")
        }
        if (actualQuantity != eventQuantity) {
            throw OrderConflictException("Offer reservation quantity does not match Order quantity")
        }
    }
}
