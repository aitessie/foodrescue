package com.example.foodrescue.orderservice.adapter.out.payments

import com.example.foodrescue.orderservice.application.payments.PaymentOperation
import com.example.foodrescue.orderservice.application.payments.PaymentResult
import com.example.foodrescue.orderservice.application.payments.PaymentResultStatus
import com.example.foodrescue.orderservice.application.ports.PaymentCommandPort
import com.example.foodrescue.orderservice.application.usecases.ProcessPaymentResultUseCase
import com.example.foodrescue.orderservice.domain.entities.OrderId
import org.springframework.stereotype.Component

@Component
class PaymentAdapter(private val processPaymentResultUseCase: ProcessPaymentResultUseCase) :
    PaymentCommandPort {
    // Temporary local payment implementation. Will be replaced by Payment Service integration.
    override fun requestAuthorization(
        orderId: OrderId,
        amount: Long,
    ) {
        processSuccessfulResult(
            orderId = orderId,
            operation = PaymentOperation.AUTHORIZATION,
            amount = amount,
        )
    }

    override fun requestCapture(
        orderId: OrderId,
        amount: Long,
    ) {
        processSuccessfulResult(
            orderId = orderId,
            operation = PaymentOperation.CAPTURE,
            amount = amount,
        )
    }

    override fun requestVoid(
        orderId: OrderId,
        amount: Long,
    ) {
        processSuccessfulResult(
            orderId = orderId,
            operation = PaymentOperation.VOID,
            amount = amount,
        )
    }

    override fun requestRefund(
        orderId: OrderId,
        amount: Long,
    ) {
        processSuccessfulResult(
            orderId = orderId,
            operation = PaymentOperation.REFUND,
            amount = amount,
        )
    }

    private fun processSuccessfulResult(
        orderId: OrderId,
        operation: PaymentOperation,
        amount: Long,
    ) {
        processPaymentResultUseCase.execute(
            PaymentResult(
                orderId = orderId,
                operation = operation,
                status = PaymentResultStatus.SUCCEEDED,
                amount = amount,
            )
        )
    }
}
