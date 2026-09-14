package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.access.OrderAccessPolicy
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetOrderUseCase(
    private val orderDBPort: OrderDBPort,
    private val orderAccessPolicy: OrderAccessPolicy,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun execute(orderId: OrderId): Order {
        logger.info("Trying to get order: orderId={}", orderId.value)

        val order = orderDBPort.findById(orderId)
        if (order == null) {
            throw OrderNotFoundException(orderId)
        }

        orderAccessPolicy.checkReadAccess(order)

        logger.info(
            "Order retrieved successfully: orderId={}, status={}",
            order.id.value,
            order.status,
        )
        return order
    }
}
