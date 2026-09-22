package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.exceptions.OrderValidationException
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.StoreId
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MarkNoShowOrdersUseCaseTest {
    @Mock private lateinit var orderDBPort: OrderDBPort

    @Mock private lateinit var processor: MarkNoShowOrderProcessor

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: MarkNoShowOrdersUseCase

    @Test
    fun whenCandidatesAreProcessed_returnsMarkedOrdersCount() {
        // Arrange
        val now = Instant.parse("2026-08-20T14:00:00Z")
        val firstOrder = createOrder()
        val secondOrder = createOrder()
        val thirdOrder = createOrder()

        `when`(clock.instant()).thenReturn(now)
        `when`(
            orderDBPort.findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 3,
            )
        )
            .thenReturn(
                listOf(
                    firstOrder,
                    secondOrder,
                    thirdOrder,
                )
            )
        `when`(
            processor.markIfExpired(
                orderId = firstOrder.id,
                now = now,
            )
        )
            .thenReturn(true)
        `when`(
            processor.markIfExpired(
                orderId = secondOrder.id,
                now = now,
            )
        )
            .thenReturn(false)
        `when`(
            processor.markIfExpired(
                orderId = thirdOrder.id,
                now = now,
            )
        )
            .thenReturn(true)

        // Act
        val result = useCase.execute(batchSize = 3)

        // Assert
        assertThat(result).isEqualTo(2)

        verify(clock).instant()
        verify(orderDBPort)
            .findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 3,
            )
        verify(processor)
            .markIfExpired(
                orderId = firstOrder.id,
                now = now,
            )
        verify(processor)
            .markIfExpired(
                orderId = secondOrder.id,
                now = now,
            )
        verify(processor)
            .markIfExpired(
                orderId = thirdOrder.id,
                now = now,
            )
        verifyNoMoreInteractions(
            orderDBPort,
            processor,
            clock,
        )
    }

    @Test
    fun whenNoCandidatesExist_returnsZero() {
        // Arrange
        val now = Instant.parse("2026-08-20T14:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
            orderDBPort.findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 10,
            )
        )
            .thenReturn(emptyList())

        // Act
        val result = useCase.execute(batchSize = 10)

        // Assert
        assertThat(result).isZero()

        verify(clock).instant()
        verify(orderDBPort)
            .findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 10,
            )
        verifyNoInteractions(processor)
        verifyNoMoreInteractions(
            orderDBPort,
            clock,
        )
    }

    @Test
    fun whenNoCandidateIsMarked_returnsZero() {
        // Arrange
        val now = Instant.parse("2026-08-20T14:00:00Z")
        val firstOrder = createOrder()
        val secondOrder = createOrder()

        `when`(clock.instant()).thenReturn(now)
        `when`(
            orderDBPort.findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 2,
            )
        )
            .thenReturn(
                listOf(
                    firstOrder,
                    secondOrder,
                )
            )
        `when`(
            processor.markIfExpired(
                orderId = firstOrder.id,
                now = now,
            )
        )
            .thenReturn(false)
        `when`(
            processor.markIfExpired(
                orderId = secondOrder.id,
                now = now,
            )
        )
            .thenReturn(false)

        // Act
        val result = useCase.execute(batchSize = 2)

        // Assert
        assertThat(result).isZero()

        verify(clock).instant()
        verify(orderDBPort)
            .findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 2,
            )
        verify(processor)
            .markIfExpired(
                orderId = firstOrder.id,
                now = now,
            )
        verify(processor)
            .markIfExpired(
                orderId = secondOrder.id,
                now = now,
            )
        verifyNoMoreInteractions(
            orderDBPort,
            processor,
            clock,
        )
    }

    @Test
    fun whenProcessorFailsForOneOrder_continuesProcessingRemainingOrders() {
        // Arrange
        val now = Instant.parse("2026-08-20T14:00:00Z")
        val firstOrder = createOrder()
        val secondOrder = createOrder()
        val thirdOrder = createOrder()

        `when`(clock.instant()).thenReturn(now)
        `when`(
            orderDBPort.findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 3,
            )
        )
            .thenReturn(
                listOf(
                    firstOrder,
                    secondOrder,
                    thirdOrder,
                )
            )
        `when`(
            processor.markIfExpired(
                orderId = firstOrder.id,
                now = now,
            )
        )
            .thenReturn(true)
        doThrow(RuntimeException("Processing failed"))
            .`when`(processor)
            .markIfExpired(
                orderId = secondOrder.id,
                now = now,
            )
        `when`(
            processor.markIfExpired(
                orderId = thirdOrder.id,
                now = now,
            )
        )
            .thenReturn(true)

        // Act
        val result = useCase.execute(batchSize = 3)

        // Assert
        assertThat(result).isEqualTo(2)

        verify(clock).instant()
        verify(orderDBPort)
            .findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 3,
            )
        verify(processor)
            .markIfExpired(
                orderId = firstOrder.id,
                now = now,
            )
        verify(processor)
            .markIfExpired(
                orderId = secondOrder.id,
                now = now,
            )
        verify(processor)
            .markIfExpired(
                orderId = thirdOrder.id,
                now = now,
            )
        verifyNoMoreInteractions(
            orderDBPort,
            processor,
            clock,
        )
    }

    @Test
    fun whenProcessorFailsForAllOrders_returnsZero() {
        // Arrange
        val now = Instant.parse("2026-08-20T14:00:00Z")
        val firstOrder = createOrder()
        val secondOrder = createOrder()

        `when`(clock.instant()).thenReturn(now)
        `when`(
            orderDBPort.findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 2,
            )
        )
            .thenReturn(
                listOf(
                    firstOrder,
                    secondOrder,
                )
            )
        doThrow(RuntimeException("First processing failed"))
            .`when`(processor)
            .markIfExpired(
                orderId = firstOrder.id,
                now = now,
            )
        doThrow(RuntimeException("Second processing failed"))
            .`when`(processor)
            .markIfExpired(
                orderId = secondOrder.id,
                now = now,
            )

        // Act
        val result = useCase.execute(batchSize = 2)

        // Assert
        assertThat(result).isZero()

        verify(clock).instant()
        verify(orderDBPort)
            .findNoShowCandidates(
                pickupEndedAt = now,
                batchSize = 2,
            )
        verify(processor)
            .markIfExpired(
                orderId = firstOrder.id,
                now = now,
            )
        verify(processor)
            .markIfExpired(
                orderId = secondOrder.id,
                now = now,
            )
        verifyNoMoreInteractions(
            orderDBPort,
            processor,
            clock,
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun whenBatchSizeIsNotPositive_throwsOrderValidationException(batchSize: Int) {
        // Arrange

        // Act
        val exception =
            assertThrows<OrderValidationException> {
                useCase.execute(batchSize = batchSize)
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("No-show batchSize must be greater than zero")

        verifyNoInteractions(
            orderDBPort,
            processor,
            clock,
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
