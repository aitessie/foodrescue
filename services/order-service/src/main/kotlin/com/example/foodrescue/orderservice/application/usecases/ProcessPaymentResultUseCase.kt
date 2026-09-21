package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.events.ApplicationEventFactory
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.payments.PaymentOperation
import com.example.foodrescue.orderservice.application.payments.PaymentResult
import com.example.foodrescue.orderservice.application.payments.PaymentResultStatus
import com.example.foodrescue.orderservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Clock
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProcessPaymentResultUseCase(
    private val orderDBPort: OrderDBPort,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(result: PaymentResult) {
        logger.info(
            "Trying to process payment result: orderId={}, operation={}, result={}",
            result.orderId.value,
            result.operation,
            result.status,
        )

        val order = orderDBPort.findById(result.orderId) ?: throw OrderNotFoundException(result.orderId)
        validateAmount(
            order = order,
            amount = result.amount,
        )

        val now = clock.instant()

        when (result.operation) {
            PaymentOperation.AUTHORIZATION -> processAuthorization(
                order = order,
                resultStatus = result.status,
                now = now,
            )

            PaymentOperation.CAPTURE -> processCapture(
                order = order,
                resultStatus = result.status,
                now = now,
            )

            PaymentOperation.VOID,
            PaymentOperation.REFUND ->
                throw OrderConflictException(
                    "Payment operation ${result.operation} cannot be processed for Order in status ${order.status}"
                )
        }

        logger.info(
            "Payment result processed successfully: orderId={}, operation={}, result={}, status={}",
            order.id.value,
            result.operation,
            result.status,
            order.status,
        )
    }

    private fun processAuthorization(
        order: Order,
        resultStatus: PaymentResultStatus,
        now: Instant,
    ) {
        when (resultStatus) {
            PaymentResultStatus.SUCCEEDED -> authorizeSuccessfully(order, now)
            PaymentResultStatus.FAILED -> failAuthorization(order, now)
        }
    }

    private fun authorizeSuccessfully(
        order: Order,
        now: Instant,
    ) {
        if (order.status == OrderStatus.RESERVED) {
            return
        }

        if (order.status != OrderStatus.PENDING) {
            throw OrderConflictException(
                "Payment authorization cannot succeed for Order in status ${order.status}"
            )
        }

        order.status = OrderStatus.RESERVED
        order.updatedAt = now
        val savedOrder = orderDBPort.save(order)

        eventPublisherPort.publish(
            eventFactory.orderReservationCommitRequested(
                order = savedOrder,
                occurredAt = now,
            )
        )
    }

    private fun processCapture(
        order: Order,
        resultStatus: PaymentResultStatus,
        now: Instant,
    ) {
        when (resultStatus) {
            PaymentResultStatus.SUCCEEDED -> captureSuccessfully(order, now)
            PaymentResultStatus.FAILED -> handleCaptureFailure(order)
        }
    }

    private fun captureSuccessfully(
        order: Order,
        now: Instant,
    ) {
        if (order.status == OrderStatus.COMPLETED) {
            return
        }

        if (order.status != OrderStatus.PICKED_UP) {
            throw OrderConflictException(
                "Payment capture cannot succeed for Order in status ${order.status}"
            )
        }

        order.status = OrderStatus.COMPLETED
        order.updatedAt = now
        orderDBPort.save(order)
    }

    private fun handleCaptureFailure(order: Order) {
        if (order.status != OrderStatus.PICKED_UP) {
            throw OrderConflictException(
                "Payment capture cannot fail for Order in status ${order.status}"
            )
        }
    }

    private fun failAuthorization(
        order: Order,
        now: Instant,
    ) {
        if (order.status == OrderStatus.FAILED) {
            return
        }

        if (order.status != OrderStatus.PENDING) {
            throw OrderConflictException(
                "Payment authorization cannot fail for Order in status ${order.status}"
            )
        }

        order.status = OrderStatus.FAILED
        order.updatedAt = now
        val savedOrder = orderDBPort.save(order)

        eventPublisherPort.publish(
            eventFactory.orderReservationReleaseRequested(
                order = savedOrder,
                occurredAt = now,
            )
        )
    }

    private fun validateAmount(
        order: Order,
        amount: Long,
    ) {
        if (amount <= 0) {
            throw OrderConflictException("Payment amount must be greater than zero")
        }
        if (amount != order.totalAmount) {
            throw OrderConflictException("Payment amount does not match Order total amount")
        }
    }
}
