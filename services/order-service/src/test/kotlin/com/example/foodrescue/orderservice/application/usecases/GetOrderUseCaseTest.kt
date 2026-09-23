package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.access.OrderAccessPolicy
import com.example.foodrescue.orderservice.application.exceptions.OrderAccessDeniedException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.StoreId
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GetOrderUseCaseTest {
    @Mock private lateinit var orderDBPort: OrderDBPort

    @Mock private lateinit var orderAccessPolicy: OrderAccessPolicy

    @InjectMocks private lateinit var useCase: GetOrderUseCase

    @Test
    fun whenOrderExistsAndAccessIsAllowed_returnsOrder() {
        // Arrange
        val order = createOrder()

        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val result = useCase.execute(orderId = order.id)

        // Assert
        assertThat(result).isSameAs(order)

        verify(orderDBPort).findById(order.id)
        verify(orderAccessPolicy).checkReadAccess(order)
        verifyNoMoreInteractions(
            orderDBPort,
            orderAccessPolicy,
        )
    }

    @Test
    fun whenOrderDoesNotExist_throwsOrderNotFoundException() {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())

        `when`(orderDBPort.findById(orderId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OrderNotFoundException> {
                useCase.execute(orderId = orderId)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order not found: ${orderId.value}")

        verify(orderDBPort).findById(orderId)
        verifyNoInteractions(orderAccessPolicy)
        verifyNoMoreInteractions(orderDBPort)
    }

    @Test
    fun whenUserHasNoReadAccess_throwsOrderAccessDeniedException() {
        // Arrange
        val order = createOrder()

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        doThrow(OrderAccessDeniedException()).`when`(orderAccessPolicy).checkReadAccess(order)

        // Act
        val exception =
            assertThrows<OrderAccessDeniedException> {
                useCase.execute(orderId = order.id)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user has no access to this Order")

        verify(orderDBPort).findById(order.id)
        verify(orderAccessPolicy).checkReadAccess(order)
        verifyNoMoreInteractions(
            orderDBPort,
            orderAccessPolicy,
        )
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
        status: OrderStatus = OrderStatus.PENDING,
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
