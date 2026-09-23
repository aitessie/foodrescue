package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.events.ApplicationEventFactory
import com.example.foodrescue.orderservice.application.events.OfferReservationResult
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.orderservice.application.ports.InboxEventDBPort
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PaymentCommandPort
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
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ProcessOfferReservationResultUseCaseTest {
    @Mock private lateinit var orderDBPort: OrderDBPort

    @Mock private lateinit var inboxEventDBPort: InboxEventDBPort

    @Mock private lateinit var paymentCommandPort: PaymentCommandPort

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: ProcessOfferReservationResultUseCase

    @Test
    fun whenHeldReservationIsProcessedForPendingOrder_requestsPaymentAuthorization() {
        // Arrange
        val order = createOrder()
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        useCase.execute(
            eventId = eventId,
            eventType = EVENT_TYPE,
            aggregateId = aggregateId,
            orderId = order.id,
            offerId = order.offerId,
            quantity = order.quantity,
            result = OfferReservationResult.HELD,
        )

        // Assert
        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(order.id)
        verify(paymentCommandPort)
            .requestAuthorization(
                orderId = order.id,
                amount = order.totalAmount,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
            paymentCommandPort,
            clock,
        )
    }

    @Test
    fun whenHeldReservationIsProcessedForCancelledOrder_requestsReservationRelease() {
        // Arrange
        val order = createOrder(status = OrderStatus.CANCELLED)
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val event =
            ApplicationEventFactory()
                .orderReservationReleaseRequested(
                    order = order,
                    occurredAt = now,
                )

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(
                eventFactory.orderReservationReleaseRequested(
                    order = order,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        useCase.execute(
            eventId = eventId,
            eventType = EVENT_TYPE,
            aggregateId = aggregateId,
            orderId = order.id,
            offerId = order.offerId,
            quantity = order.quantity,
            result = OfferReservationResult.HELD,
        )

        // Assert
        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(order.id)
        verify(eventFactory)
            .orderReservationReleaseRequested(
                order = order,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoInteractions(paymentCommandPort)
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["PENDING", "CANCELLED"],
    )
    fun whenHeldReservationIsProcessedForInvalidOrderStatus_throwsOrderConflictException(
        status: OrderStatus
    ) {
        // Arrange
        val order = createOrder(status = status)
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    orderId = order.id,
                    offerId = order.offerId,
                    quantity = order.quantity,
                    result = OfferReservationResult.HELD,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Offer reservation cannot be held for Order in status $status")

        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
            clock,
        )
    }

    @Test
    fun whenRejectedReservationIsProcessedForPendingOrder_marksOrderAsFailed() {
        // Arrange
        val order = createOrder()
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(orderDBPort.save(order)).thenReturn(order)

        // Act
        useCase.execute(
            eventId = eventId,
            eventType = EVENT_TYPE,
            aggregateId = aggregateId,
            orderId = order.id,
            offerId = order.offerId,
            quantity = order.quantity,
            result = OfferReservationResult.REJECTED,
        )

        // Assert
        assertThat(order.status).isEqualTo(OrderStatus.FAILED)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(order.id)
        verify(orderDBPort).save(order)
        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
            clock,
        )
    }

    @Test
    fun whenRejectedReservationIsProcessedForCancelledOrder_ignoresResult() {
        // Arrange
        val order = createOrder(status = OrderStatus.CANCELLED)
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        useCase.execute(
            eventId = eventId,
            eventType = EVENT_TYPE,
            aggregateId = aggregateId,
            orderId = order.id,
            offerId = order.offerId,
            quantity = order.quantity,
            result = OfferReservationResult.REJECTED,
        )

        // Assert
        assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)

        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
            clock,
        )
    }

    @Test
    fun whenRejectedReservationIsProcessedForFailedOrder_ignoresResult() {
        // Arrange
        val order = createOrder(status = OrderStatus.FAILED)
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        useCase.execute(
            eventId = eventId,
            eventType = EVENT_TYPE,
            aggregateId = aggregateId,
            orderId = order.id,
            offerId = order.offerId,
            quantity = order.quantity,
            result = OfferReservationResult.REJECTED,
        )

        // Assert
        assertThat(order.status).isEqualTo(OrderStatus.FAILED)

        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
            clock,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["PENDING", "CANCELLED", "FAILED"],
    )
    fun whenRejectedReservationIsProcessedForInvalidOrderStatus_throwsOrderConflictException(
        status: OrderStatus
    ) {
        // Arrange
        val order = createOrder(status = status)
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    orderId = order.id,
                    offerId = order.offerId,
                    quantity = order.quantity,
                    result = OfferReservationResult.REJECTED,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Offer reservation cannot be rejected for Order in status $status")

        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
            clock,
        )
    }

    @Test
    fun whenEventWasAlreadyProcessed_doesNothing() {
        // Arrange
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val orderId = OrderId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(false)

        // Act
        useCase.execute(
            eventId = eventId,
            eventType = EVENT_TYPE,
            aggregateId = aggregateId,
            orderId = orderId,
            offerId = offerId,
            quantity = 2,
            result = OfferReservationResult.HELD,
        )

        // Assert
        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verifyNoInteractions(
            orderDBPort,
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            inboxEventDBPort,
            clock,
        )
    }

    @Test
    fun whenOrderDoesNotExist_throwsOrderNotFoundException() {
        // Arrange
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val orderId = OrderId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(orderId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OrderNotFoundException> {
                useCase.execute(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    orderId = orderId,
                    offerId = offerId,
                    quantity = 2,
                    result = OfferReservationResult.HELD,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order not found: ${orderId.value}")

        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(orderId)
        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
            clock,
        )
    }

    @Test
    fun whenReservationResultBelongsToAnotherOffer_throwsOrderConflictException() {
        // Arrange
        val order = createOrder()
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val eventOfferId = OfferId(UUID.randomUUID())
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    orderId = order.id,
                    offerId = eventOfferId,
                    quantity = order.quantity,
                    result = OfferReservationResult.HELD,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Offer reservation result belongs to another Offer")

        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
            clock,
        )
    }

    @Test
    fun whenReservationQuantityDoesNotMatchOrderQuantity_throwsOrderConflictException() {
        // Arrange
        val order = createOrder()
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val eventQuantity = order.quantity + 1
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                inboxEventDBPort.tryMarkProcessed(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    processedAt = now,
                )
            )
            .thenReturn(true)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    eventId = eventId,
                    eventType = EVENT_TYPE,
                    aggregateId = aggregateId,
                    orderId = order.id,
                    offerId = order.offerId,
                    quantity = eventQuantity,
                    result = OfferReservationResult.HELD,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Offer reservation quantity does not match Order quantity")

        verify(clock).instant()
        verify(inboxEventDBPort)
            .tryMarkProcessed(
                eventId = eventId,
                eventType = EVENT_TYPE,
                aggregateId = aggregateId,
                processedAt = now,
            )
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            inboxEventDBPort,
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
        private const val EVENT_TYPE = "offer.reservation.result"
    }
}
