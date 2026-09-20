package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.access.OrderAccessPolicy
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PickupTokenDBPort
import com.example.foodrescue.orderservice.application.ports.PickupTokenGeneratorPort
import com.example.foodrescue.orderservice.application.ports.PickupTokenHashPort
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.PickupToken
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetPickupTokenUseCase(
    private val orderDBPort: OrderDBPort,
    private val pickupTokenDBPort: PickupTokenDBPort,
    private val pickupTokenGeneratorPort: PickupTokenGeneratorPort,
    private val pickupTokenHashPort: PickupTokenHashPort,
    private val orderAccessPolicy: OrderAccessPolicy,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(orderId: OrderId): String {
        logger.info("Trying to get pickup token: orderId={}", orderId.value)

        val order = orderDBPort.findById(orderId) ?: throw OrderNotFoundException(orderId)
        orderAccessPolicy.checkReadAccess(order)

        if (order.status != OrderStatus.RESERVED) {
            throw OrderConflictException("Pickup token is available only for a reserved Order")
        }

        val existingToken = pickupTokenDBPort.findByOrderId(orderId)
        if (existingToken?.usedAt != null) {
            throw OrderConflictException("Pickup token has already been used")
        }

        val rawToken = pickupTokenGeneratorPort.generate()
        val now = clock.instant()
        val pickupToken =
            PickupToken(
                orderId = orderId,
                tokenHash = pickupTokenHashPort.hash(rawToken),
                usedAt = null,
                version = existingToken?.version ?: 0L,
                createdAt = existingToken?.createdAt ?: now,
                updatedAt = now,
            )

        pickupTokenDBPort.save(pickupToken)

        logger.info("Pickup token issued successfully: orderId={}", orderId.value)
        return rawToken
    }
}
