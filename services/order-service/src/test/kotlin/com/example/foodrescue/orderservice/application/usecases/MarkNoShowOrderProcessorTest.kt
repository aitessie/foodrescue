package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PaymentCommandPort
import com.example.foodrescue.orderservice.configuration.NoShowPaymentAction
import com.example.foodrescue.orderservice.configuration.OrderSchedulerProperties
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.StoreId
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MarkNoShowOrderProcessorTest {
    @Mock private lateinit var orderDBPort: OrderDBPort

    @Mock private lateinit var paymentCommandPort: PaymentCommandPort

    @Mock private lateinit var schedulerProperties: OrderSchedulerProperties

    @InjectMocks private lateinit var processor: MarkNoShowOrderProcessor

    @Test
    fun whenReservedOrderIsExpiredAndPaymentActionIsCapture_marksOrderAsNoShowAndRequestsCapture() {
        // Arrange
        val now = Instant.parse("2026-08-20T14:00:00Z")
        val order = createOrder(pickupEnd = Instant.parse("2026-08-20T13:00:00Z"))
        val savedOrder =
            createOrder(
                id = order.id,
                customerId = order.customerId,
                offerId = order.offerId,
                storeId = order.storeId,
                quantity = order.quantity,
                unitPrice = order.unitPrice,
                totalAmount = order.totalAmount,
                pickupStart = order.pickupStart,
                pickupEnd = order.pickupEnd,
                status = OrderStatus.NO_SHOW,
                version = 1,
                createdAt = order.createdAt,
                updatedAt = now,
            )

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(orderDBPort.save(order)).thenReturn(savedOrder)
        `when`(schedulerProperties.noShowPaymentAction).thenReturn(NoShowPaymentAction.CAPTURE)

        // Act
        val result =
            processor.markIfExpired(
                orderId = order.id,
                now = now,
            )

        // Assert
        assertThat(result).isTrue()
        assertThat(order.status).isEqualTo(OrderStatus.NO_SHOW)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(orderDBPort).findById(order.id)
        verify(orderDBPort).save(order)
        verify(schedulerProperties, times(2)).noShowPaymentAction
        verify(paymentCommandPort)
            .requestCapture(
                orderId = savedOrder.id,
                amount = savedOrder.totalAmount,
            )
        verify(paymentCommandPort, never())
            .requestVoid(
                orderId = savedOrder.id,
                amount = savedOrder.totalAmount,
            )
        verifyNoMoreInteractions(
            orderDBPort,
            paymentCommandPort,
            schedulerProperties,
        )
    }

    @Test
    fun whenReservedOrderIsExpiredAndPaymentActionIsVoid_marksOrderAsNoShowAndRequestsVoid() {
        // Arrange
        val now = Instant.parse("2026-08-20T14:00:00Z")
        val order = createOrder(pickupEnd = Instant.parse("2026-08-20T13:00:00Z"))
        val savedOrder =
            createOrder(
                id = order.id,
                customerId = order.customerId,
                offerId = order.offerId,
                storeId = order.storeId,
                quantity = order.quantity,
                unitPrice = order.unitPrice,
                totalAmount = order.totalAmount,
                pickupStart = order.pickupStart,
                pickupEnd = order.pickupEnd,
                status = OrderStatus.NO_SHOW,
                version = 1,
                createdAt = order.createdAt,
                updatedAt = now,
            )

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(orderDBPort.save(order)).thenReturn(savedOrder)
        `when`(schedulerProperties.noShowPaymentAction).thenReturn(NoShowPaymentAction.VOID)

        // Act
        val result =
            processor.markIfExpired(
                orderId = order.id,
                now = now,
            )

        // Assert
        assertThat(result).isTrue()
        assertThat(order.status).isEqualTo(OrderStatus.NO_SHOW)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(orderDBPort).findById(order.id)
        verify(orderDBPort).save(order)
        verify(schedulerProperties, times(2)).noShowPaymentAction
        verify(paymentCommandPort)
            .requestVoid(
                orderId = savedOrder.id,
                amount = savedOrder.totalAmount,
            )
        verify(paymentCommandPort, never())
            .requestCapture(
                orderId = savedOrder.id,
                amount = savedOrder.totalAmount,
            )
        verifyNoMoreInteractions(
            orderDBPort,
            paymentCommandPort,
            schedulerProperties,
        )
    }

    @Test
    fun whenPickupWindowEndsAtCurrentTime_marksOrderAsNoShow() {
        // Arrange
        val now = Instant.parse("2026-08-20T14:00:00Z")
        val order = createOrder(pickupEnd = now)
        val savedOrder =
            createOrder(
                id = order.id,
                customerId = order.customerId,
                offerId = order.offerId,
                storeId = order.storeId,
                quantity = order.quantity,
                unitPrice = order.unitPrice,
                totalAmount = order.totalAmount,
                pickupStart = order.pickupStart,
                pickupEnd = order.pickupEnd,
                status = OrderStatus.NO_SHOW,
                version = 1,
                createdAt = order.createdAt,
                updatedAt = now,
            )

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(orderDBPort.save(order)).thenReturn(savedOrder)
        `when`(schedulerProperties.noShowPaymentAction).thenReturn(NoShowPaymentAction.CAPTURE)

        // Act
        val result =
            processor.markIfExpired(
                orderId = order.id,
                now = now,
            )

        // Assert
        assertThat(result).isTrue()
        assertThat(order.status).isEqualTo(OrderStatus.NO_SHOW)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(orderDBPort).findById(order.id)
        verify(orderDBPort).save(order)
        verify(schedulerProperties, times(2)).noShowPaymentAction
        verify(paymentCommandPort)
            .requestCapture(
                orderId = savedOrder.id,
                amount = savedOrder.totalAmount,
            )
        verifyNoMoreInteractions(
            orderDBPort,
            paymentCommandPort,
            schedulerProperties,
        )
    }

    @Test
    fun whenOrderDoesNotExist_returnsFalse() {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val now = Instant.parse("2026-08-20T14:00:00Z")

        `when`(orderDBPort.findById(orderId)).thenReturn(null)

        // Act
        val result =
            processor.markIfExpired(
                orderId = orderId,
                now = now,
            )

        // Assert
        assertThat(result).isFalse()

        verify(orderDBPort).findById(orderId)
        verifyNoInteractions(
            paymentCommandPort,
            schedulerProperties,
        )
        verifyNoMoreInteractions(orderDBPort)
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["RESERVED"],
    )
    fun whenOrderIsNotReserved_returnsFalse(status: OrderStatus) {
        // Arrange
        val now = Instant.parse("2026-08-20T14:00:00Z")
        val order =
            createOrder(
                status = status,
                pickupEnd = Instant.parse("2026-08-20T13:00:00Z"),
            )

        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val result =
            processor.markIfExpired(
                orderId = order.id,
                now = now,
            )

        // Assert
        assertThat(result).isFalse()
        assertThat(order.status).isEqualTo(status)
        assertThat(order.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            paymentCommandPort,
            schedulerProperties,
        )
        verifyNoMoreInteractions(orderDBPort)
    }

    @Test
    fun whenReservedOrderPickupWindowHasNotEnded_returnsFalse() {
        // Arrange
        val now = Instant.parse("2026-08-20T13:00:00Z")
        val order = createOrder(pickupEnd = Instant.parse("2026-08-20T14:00:00Z"))

        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val result =
            processor.markIfExpired(
                orderId = order.id,
                now = now,
            )

        // Assert
        assertThat(result).isFalse()
        assertThat(order.status).isEqualTo(OrderStatus.RESERVED)
        assertThat(order.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            paymentCommandPort,
            schedulerProperties,
        )
        verifyNoMoreInteractions(orderDBPort)
    }

    private fun createOrder(
        id: OrderId = OrderId(UUID.randomUUID()),
        customerId: String = CUSTOMER_ID,
        offerId: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        quantity: Int = 2,
        unitPrice: Long = 500,
        totalAmount: Long = 1000,
        pickupStart: Instant = Instant.parse("2026-08-20T12:00:00Z"),
        pickupEnd: Instant = Instant.parse("2026-08-20T14:00:00Z"),
        status: OrderStatus = OrderStatus.RESERVED,
        version: Long = 0,
        createdAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
    ): Order =
        Order(
            id = id,
            customerId = customerId,
            offerId = offerId,
            storeId = storeId,
            quantity = quantity,
            unitPrice = unitPrice,
            totalAmount = totalAmount,
            pickupStart = pickupStart,
            pickupEnd = pickupEnd,
            status = status,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        private const val CUSTOMER_ID = "33333333-3333-3333-3333-333333333333"
    }
}
