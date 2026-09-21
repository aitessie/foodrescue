package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.access.PartnerStoreAccessPolicy
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.exceptions.PickupTokenNotFoundException
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PaymentCommandPort
import com.example.foodrescue.orderservice.application.ports.PickupTokenDBPort
import com.example.foodrescue.orderservice.application.ports.PickupTokenHashPort
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.PickupToken
import com.example.foodrescue.orderservice.domain.entities.StoreId
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Clock
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConfirmPickupUseCase(
    private val pickupTokenDBPort: PickupTokenDBPort,
    private val pickupTokenHashPort: PickupTokenHashPort,
    private val orderDBPort: OrderDBPort,
    private val partnerStoreAccessPolicy: PartnerStoreAccessPolicy,
    private val paymentCommandPort: PaymentCommandPort,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(
        storeId: StoreId,
        rawToken: String,
    ): Order {
        logger.info("Trying to confirm pickup: storeId={}", storeId.value)

        if (rawToken.isBlank()) {
            throw PickupTokenNotFoundException()
        }

        val tokenHash = pickupTokenHashPort.hash(rawToken)
        val pickupToken =
            pickupTokenDBPort.findByTokenHash(tokenHash) ?: throw PickupTokenNotFoundException()
        val order =
            orderDBPort.findById(pickupToken.orderId)
                ?: throw OrderNotFoundException(pickupToken.orderId)

        if (order.storeId != storeId) {
            throw PickupTokenNotFoundException()
        }

        partnerStoreAccessPolicy.checkAccess(storeId)

        if (pickupToken.usedAt != null) {
            if (order.status == OrderStatus.PICKED_UP || order.status == OrderStatus.COMPLETED) {
                logger.info(
                    "Pickup confirmation returned idempotently: orderId={}, storeId={}, status={}",
                    order.id.value,
                    storeId.value,
                    order.status,
                )
                return order
            }

            throw OrderConflictException("Pickup token has already been used")
        }

        if (order.status != OrderStatus.RESERVED) {
            throw OrderConflictException(
                "Pickup cannot be confirmed for Order in status ${order.status}"
            )
        }

        val now = clock.instant()
        validatePickupWindow(
            order = order,
            now = now,
        )

        pickupTokenDBPort.save(
            PickupToken(
                orderId = pickupToken.orderId,
                tokenHash = pickupToken.tokenHash,
                usedAt = now,
                version = pickupToken.version,
                createdAt = pickupToken.createdAt,
                updatedAt = now,
            )
        )

        order.status = OrderStatus.PICKED_UP
        order.updatedAt = now
        val pickedUpOrder = orderDBPort.save(order)

        paymentCommandPort.requestCapture(
            orderId = pickedUpOrder.id,
            amount = pickedUpOrder.totalAmount,
        )

        val result =
            orderDBPort.findById(pickedUpOrder.id) ?: throw OrderNotFoundException(pickedUpOrder.id)

        logger.info(
            "Pickup confirmed successfully: orderId={}, storeId={}, status={}",
            result.id.value,
            storeId.value,
            result.status,
        )
        return result
    }

    private fun validatePickupWindow(
        order: Order,
        now: Instant,
    ) {
        if (now.isBefore(order.pickupStart) || now.isAfter(order.pickupEnd)) {
            throw OrderConflictException(
                "Pickup can be confirmed only during the Order pickup window"
            )
        }
    }
}
