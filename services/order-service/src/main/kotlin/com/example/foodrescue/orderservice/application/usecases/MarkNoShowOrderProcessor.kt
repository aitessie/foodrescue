package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PaymentCommandPort
import com.example.foodrescue.orderservice.configuration.NoShowPaymentAction
import com.example.foodrescue.orderservice.configuration.OrderSchedulerProperties
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class MarkNoShowOrderProcessor(
    private val orderDBPort: OrderDBPort,
    private val paymentCommandPort: PaymentCommandPort,
    private val schedulerProperties: OrderSchedulerProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markIfExpired(
        orderId: OrderId,
        now: Instant,
    ): Boolean {
        val order = orderDBPort.findById(orderId) ?: return false

        if (order.status != OrderStatus.RESERVED || order.pickupEnd.isAfter(now)) {
            return false
        }

        order.status = OrderStatus.NO_SHOW
        order.updatedAt = now
        val savedOrder = orderDBPort.save(order)

        when (schedulerProperties.noShowPaymentAction) {
            NoShowPaymentAction.CAPTURE ->
                paymentCommandPort.requestCapture(
                    orderId = savedOrder.id,
                    amount = savedOrder.totalAmount,
                )

            NoShowPaymentAction.VOID ->
                paymentCommandPort.requestVoid(
                    orderId = savedOrder.id,
                    amount = savedOrder.totalAmount,
                )
        }

        logger.info(
            "Order marked as no-show: orderId={}, paymentAction={}",
            savedOrder.id.value,
            schedulerProperties.noShowPaymentAction,
        )
        return true
    }
}
