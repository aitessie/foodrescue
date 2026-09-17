package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.events.ApplicationEventFactory
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.exceptions.OrderValidationException
import com.example.foodrescue.orderservice.application.ports.CurrentUserPort
import com.example.foodrescue.orderservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.orderservice.application.ports.OfferQueryPort
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.OfferSnapshot
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.enum.OfferStatus
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.lang.Math.multiplyExact
import java.time.Clock
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateOrderUseCase(
    private val orderDBPort: OrderDBPort,
    private val offerQueryPort: OfferQueryPort,
    private val currentUserPort: CurrentUserPort,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(
        orderId: OrderId,
        offerId: OfferId,
        quantity: Int,
    ): Order {
        val customerId = currentUserPort.getUserId()

        logger.info(
            "Trying to create order: orderId={}, customerId={}, offerId={}, quantity={}",
            orderId.value,
            customerId,
            offerId.value,
            quantity,
        )

        validateQuantity(quantity)

        val existingOrder = orderDBPort.findById(orderId)
        if (existingOrder != null) {
            val order =
                validateIdempotentRequest(
                    order = existingOrder,
                    orderId = orderId,
                    customerId = customerId,
                    offerId = offerId,
                    quantity = quantity,
                )

            logger.info(
                "Existing order returned: orderId={}, status={}",
                order.id.value,
                order.status,
            )
            return order
        }

        val now = clock.instant()
        val offer = offerQueryPort.getOffer(offerId)
        validateOffer(
            offer = offer,
            quantity = quantity,
            now = now,
        )

        val order =
            Order(
                id = orderId,
                customerId = customerId,
                offerId = offer.offerId,
                storeId = offer.storeId,
                quantity = quantity,
                unitPrice = offer.unitPrice,
                totalAmount = calculateTotalAmount(offer.unitPrice, quantity),
                pickupStart = offer.pickupStart,
                pickupEnd = offer.pickupEnd,
                status = OrderStatus.PENDING,
                version = 0,
                createdAt = now,
                updatedAt = now,
            )

        val (savedOrder, created) =
            saveOrder(
                order = order,
                customerId = customerId,
                offerId = offerId,
                quantity = quantity,
            )

        if (created) {
            eventPublisherPort.publish(
                eventFactory.orderReservationRequested(
                    order = savedOrder,
                    occurredAt = now,
                )
            )
        }

        logger.info(
            "Order created successfully: orderId={}, status={}",
            savedOrder.id.value,
            savedOrder.status,
        )
        return savedOrder
    }

    private fun saveOrder(
        order: Order,
        customerId: String,
        offerId: OfferId,
        quantity: Int,
    ): Pair<Order, Boolean> =
        try {
            orderDBPort.save(order) to true
        } catch (exception: DataIntegrityViolationException) {
            val existingOrder = orderDBPort.findById(order.id) ?: throw exception

            validateIdempotentRequest(
                order = existingOrder,
                orderId = order.id,
                customerId = customerId,
                offerId = offerId,
                quantity = quantity,
            ) to false
        }

    private fun validateQuantity(quantity: Int) {
        if (quantity <= 0) {
            throw OrderValidationException("quantity must be greater than zero")
        }
    }

    private fun validateIdempotentRequest(
        order: Order,
        orderId: OrderId,
        customerId: String,
        offerId: OfferId,
        quantity: Int,
    ): Order {
        if (order.customerId != customerId) {
            throw OrderNotFoundException(orderId)
        }
        if (order.offerId != offerId) {
            throw OrderConflictException("Order already belongs to another Offer")
        }
        if (order.quantity != quantity) {
            throw OrderConflictException("Order quantity does not match the existing Order")
        }
        return order
    }

    private fun validateOffer(
        offer: OfferSnapshot,
        quantity: Int,
        now: Instant,
    ) {
        if (offer.status != OfferStatus.ACTIVE) {
            throw OrderConflictException("Offer is not active")
        }
        if (offer.availableQuantity < quantity) {
            throw OrderConflictException("Requested quantity exceeds available Offer quantity")
        }
        if (!offer.pickupEnd.isAfter(now)) {
            throw OrderConflictException("Offer pickup window has already ended")
        }
    }

    private fun calculateTotalAmount(
        unitPrice: Long,
        quantity: Int,
    ): Long =
        try {
            multiplyExact(unitPrice, quantity.toLong())
        } catch (exception: ArithmeticException) {
            throw OrderValidationException("Order total amount exceeds supported range")
        }
}
