package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.access.PartnerStoreAccessPolicy
import com.example.foodrescue.orderservice.application.events.ApplicationEventFactory
import com.example.foodrescue.orderservice.application.exceptions.OrderAccessDeniedException
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.ports.CurrentUserPort
import com.example.foodrescue.orderservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PaymentCommandPort
import com.example.foodrescue.orderservice.configuration.OrderCancellationProperties
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.enum.ApplicationRole
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Clock
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CancelOrderUseCase(
    private val orderDBPort: OrderDBPort,
    private val currentUserPort: CurrentUserPort,
    private val partnerStoreAccessPolicy: PartnerStoreAccessPolicy,
    private val paymentCommandPort: PaymentCommandPort,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val properties: OrderCancellationProperties,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(orderId: OrderId): Order {
        logger.info("Trying to cancel Order: orderId={}", orderId.value)

        val order = orderDBPort.findById(orderId) ?: throw OrderNotFoundException(orderId)
        val actor = resolveActor(order)

        if (order.status == OrderStatus.CANCELLED) {
            logger.info(
                "Order cancellation returned idempotently: orderId={}, status={}",
                order.id.value,
                order.status,
            )
            return order
        }

        validateCancellableStatus(order)

        val now = clock.instant()
        if (actor == CancellationActor.CUSTOMER) {
            validateCustomerDeadline(
                order = order,
                now = now,
            )
        }

        val previousStatus = order.status
        order.status = OrderStatus.CANCELLED
        order.updatedAt = now
        val savedOrder = orderDBPort.save(order)

        when (previousStatus) {
            OrderStatus.PENDING -> Unit
            OrderStatus.RESERVED -> cancelReservedOrder(savedOrder, now)
            else ->
                throw OrderConflictException("Order in status $previousStatus cannot be cancelled")
        }

        logger.info(
            "Order cancelled successfully: orderId={}, previousStatus={}, status={}",
            savedOrder.id.value,
            previousStatus,
            savedOrder.status,
        )
        return savedOrder
    }

    private fun resolveActor(order: Order): CancellationActor {
        if (currentUserPort.hasRole(ApplicationRole.ADMIN)) {
            return CancellationActor.ADMIN
        }

        if (
            currentUserPort.hasRole(ApplicationRole.MANAGER) ||
                currentUserPort.hasRole(ApplicationRole.STAFF)
        ) {
            partnerStoreAccessPolicy.checkCancellationAccess(order.storeId)
            return CancellationActor.STORE
        }

        if (currentUserPort.hasRole(ApplicationRole.CUSTOMER)) {
            if (order.customerId != currentUserPort.getUserId()) {
                throw OrderAccessDeniedException()
            }
            return CancellationActor.CUSTOMER
        }

        throw OrderAccessDeniedException()
    }

    private fun validateCancellableStatus(order: Order) {
        if (order.status != OrderStatus.PENDING && order.status != OrderStatus.RESERVED) {
            throw OrderConflictException("Order in status ${order.status} cannot be cancelled")
        }
    }

    private fun validateCustomerDeadline(
        order: Order,
        now: Instant,
    ) {
        val cancellationDeadline =
            order.pickupStart.minus(properties.customerDeadlineBeforePickupStart)

        if (!now.isBefore(cancellationDeadline)) {
            throw OrderConflictException("Customer cancellation deadline has passed")
        }
    }

    private fun cancelReservedOrder(
        order: Order,
        now: Instant,
    ) {
        eventPublisherPort.publish(
            eventFactory.orderReservationReleaseRequested(
                order = order,
                occurredAt = now,
            )
        )

        paymentCommandPort.requestVoid(
            orderId = order.id,
            amount = order.totalAmount,
        )
    }

    private enum class CancellationActor {
        CUSTOMER,
        STORE,
        ADMIN,
    }
}
