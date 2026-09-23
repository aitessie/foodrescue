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
import com.example.foodrescue.orderservice.domain.entities.StoreId
import com.example.foodrescue.orderservice.domain.enum.OfferStatus
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException

@ExtendWith(MockitoExtension::class)
class CreateOrderUseCaseTest {
    @Mock private lateinit var orderDBPort: OrderDBPort

    @Mock private lateinit var offerQueryPort: OfferQueryPort

    @Mock private lateinit var currentUserPort: CurrentUserPort

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: java.time.Clock

    @InjectMocks private lateinit var useCase: CreateOrderUseCase

    @Test
    fun whenValidOrderIsCreated_returnsSavedOrder() {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val offer = createOfferSnapshot()
        val quantity = 2
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedOrder =
            createOrder(
                id = orderId,
                offerId = offer.offerId,
                storeId = offer.storeId,
                quantity = quantity,
                unitPrice = offer.unitPrice,
                totalAmount = offer.unitPrice * quantity,
                pickupStart = offer.pickupStart,
                pickupEnd = offer.pickupEnd,
                createdAt = now,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .orderReservationRequested(
                    order = savedOrder,
                    occurredAt = now,
                )

        var orderToSave: Order? = null

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(orderId)).thenReturn(null)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerQueryPort.getOffer(offer.offerId)).thenReturn(offer)
        doAnswer { invocation ->
                orderToSave = invocation.getArgument(0)
                savedOrder
            }
            .`when`(orderDBPort)
            .save(anyOrder())
        `when`(
                eventFactory.orderReservationRequested(
                    order = savedOrder,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result =
            useCase.execute(
                orderId = orderId,
                offerId = offer.offerId,
                quantity = quantity,
            )

        // Assert
        assertThat(result).isSameAs(savedOrder)

        assertThat(orderToSave).isNotNull
        assertThat(orderToSave!!.id).isEqualTo(orderId)
        assertThat(orderToSave!!.customerId).isEqualTo(CURRENT_USER_ID)
        assertThat(orderToSave!!.offerId).isEqualTo(offer.offerId)
        assertThat(orderToSave!!.storeId).isEqualTo(offer.storeId)
        assertThat(orderToSave!!.quantity).isEqualTo(quantity)
        assertThat(orderToSave!!.unitPrice).isEqualTo(offer.unitPrice)
        assertThat(orderToSave!!.totalAmount).isEqualTo(offer.unitPrice * quantity)
        assertThat(orderToSave!!.pickupStart).isEqualTo(offer.pickupStart)
        assertThat(orderToSave!!.pickupEnd).isEqualTo(offer.pickupEnd)
        assertThat(orderToSave!!.status).isEqualTo(OrderStatus.PENDING)
        assertThat(orderToSave!!.version).isZero()
        assertThat(orderToSave!!.createdAt).isEqualTo(now)
        assertThat(orderToSave!!.updatedAt).isEqualTo(now)

        verify(currentUserPort).getUserId()
        verify(orderDBPort).findById(orderId)
        verify(clock).instant()
        verify(offerQueryPort).getOffer(offer.offerId)
        verify(orderDBPort).save(orderToSave!!)
        verify(eventFactory)
            .orderReservationRequested(
                order = savedOrder,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            orderDBPort,
            offerQueryPort,
            currentUserPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenSameOrderAlreadyExists_returnsExistingOrder() {
        // Arrange
        val order = createOrder()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val result =
            useCase.execute(
                orderId = order.id,
                offerId = order.offerId,
                quantity = order.quantity,
            )

        // Assert
        assertThat(result).isSameAs(order)

        verify(currentUserPort).getUserId()
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            offerQueryPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenExistingOrderBelongsToAnotherCustomer_throwsOrderNotFoundException() {
        // Arrange
        val order = createOrder(customerId = OTHER_USER_ID)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderNotFoundException> {
                useCase.execute(
                    orderId = order.id,
                    offerId = order.offerId,
                    quantity = order.quantity,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order not found: ${order.id.value}")

        verify(currentUserPort).getUserId()
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            offerQueryPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenExistingOrderBelongsToAnotherOffer_throwsOrderConflictException() {
        // Arrange
        val order = createOrder()
        val requestedOfferId = OfferId(UUID.randomUUID())

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    orderId = order.id,
                    offerId = requestedOfferId,
                    quantity = order.quantity,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order already belongs to another Offer")

        verify(currentUserPort).getUserId()
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            offerQueryPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenExistingOrderQuantityDoesNotMatch_throwsOrderConflictException() {
        // Arrange
        val order = createOrder()
        val requestedQuantity = order.quantity + 1

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    orderId = order.id,
                    offerId = order.offerId,
                    quantity = requestedQuantity,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order quantity does not match the existing Order")

        verify(currentUserPort).getUserId()
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            offerQueryPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun whenQuantityIsNotPositive_throwsOrderValidationException(quantity: Int) {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)

        // Act
        val exception =
            assertThrows<OrderValidationException> {
                useCase.execute(
                    orderId = orderId,
                    offerId = offerId,
                    quantity = quantity,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("quantity must be greater than zero")

        verify(currentUserPort).getUserId()
        verifyNoInteractions(
            orderDBPort,
            offerQueryPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(currentUserPort)
    }

    @ParameterizedTest
    @EnumSource(
        value = OfferStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["ACTIVE"],
    )
    fun whenOfferIsNotActive_throwsOrderConflictException(status: OfferStatus) {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val offer = createOfferSnapshot(status = status)
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(orderId)).thenReturn(null)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerQueryPort.getOffer(offer.offerId)).thenReturn(offer)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    orderId = orderId,
                    offerId = offer.offerId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Offer is not active")

        verify(currentUserPort).getUserId()
        verify(orderDBPort).findById(orderId)
        verify(clock).instant()
        verify(offerQueryPort).getOffer(offer.offerId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            offerQueryPort,
            currentUserPort,
            clock,
        )
    }

    @Test
    fun whenRequestedQuantityExceedsAvailableOfferQuantity_throwsOrderConflictException() {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val offer = createOfferSnapshot(availableQuantity = 1)
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(orderId)).thenReturn(null)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerQueryPort.getOffer(offer.offerId)).thenReturn(offer)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    orderId = orderId,
                    offerId = offer.offerId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Requested quantity exceeds available Offer quantity")

        verify(currentUserPort).getUserId()
        verify(orderDBPort).findById(orderId)
        verify(clock).instant()
        verify(offerQueryPort).getOffer(offer.offerId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            offerQueryPort,
            currentUserPort,
            clock,
        )
    }

    @Test
    fun whenOfferPickupWindowEndsAtCurrentTime_throwsOrderConflictException() {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val now = Instant.parse("2026-08-20T14:00:00Z")
        val offer = createOfferSnapshot(pickupEnd = now)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(orderId)).thenReturn(null)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerQueryPort.getOffer(offer.offerId)).thenReturn(offer)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    orderId = orderId,
                    offerId = offer.offerId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Offer pickup window has already ended")

        verify(currentUserPort).getUserId()
        verify(orderDBPort).findById(orderId)
        verify(clock).instant()
        verify(offerQueryPort).getOffer(offer.offerId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            offerQueryPort,
            currentUserPort,
            clock,
        )
    }

    @Test
    fun whenOrderTotalAmountExceedsLongRange_throwsOrderValidationException() {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val offer =
            createOfferSnapshot(
                unitPrice = Long.MAX_VALUE,
                availableQuantity = 2,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(orderId)).thenReturn(null)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerQueryPort.getOffer(offer.offerId)).thenReturn(offer)

        // Act
        val exception =
            assertThrows<OrderValidationException> {
                useCase.execute(
                    orderId = orderId,
                    offerId = offer.offerId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order total amount exceeds supported range")

        verify(currentUserPort).getUserId()
        verify(orderDBPort).findById(orderId)
        verify(clock).instant()
        verify(offerQueryPort).getOffer(offer.offerId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            offerQueryPort,
            currentUserPort,
            clock,
        )
    }

    @Test
    fun whenConcurrentCreateFindsSameOrder_returnsExistingOrder() {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val offer = createOfferSnapshot()
        val quantity = 2
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val existingOrder =
            createOrder(
                id = orderId,
                offerId = offer.offerId,
                storeId = offer.storeId,
                quantity = quantity,
                unitPrice = offer.unitPrice,
                totalAmount = offer.unitPrice * quantity,
                pickupStart = offer.pickupStart,
                pickupEnd = offer.pickupEnd,
            )

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(orderId)).thenReturn(null).thenReturn(existingOrder)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerQueryPort.getOffer(offer.offerId)).thenReturn(offer)
        doThrow(DataIntegrityViolationException("duplicate order"))
            .`when`(orderDBPort)
            .save(anyOrder())

        // Act
        val result =
            useCase.execute(
                orderId = orderId,
                offerId = offer.offerId,
                quantity = quantity,
            )

        // Assert
        assertThat(result).isSameAs(existingOrder)

        verify(currentUserPort).getUserId()
        verify(orderDBPort, times(2)).findById(orderId)
        verify(clock).instant()
        verify(offerQueryPort).getOffer(offer.offerId)
        verify(orderDBPort).save(anyOrder())
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            offerQueryPort,
            currentUserPort,
            clock,
        )
    }

    @Test
    fun whenConcurrentCreateFindsOrderWithDifferentOffer_throwsOrderConflictException() {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val offer = createOfferSnapshot()
        val existingOrder =
            createOrder(
                id = orderId,
                offerId = OfferId(UUID.randomUUID()),
                quantity = 2,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(orderId)).thenReturn(null).thenReturn(existingOrder)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerQueryPort.getOffer(offer.offerId)).thenReturn(offer)
        doThrow(DataIntegrityViolationException("duplicate order"))
            .`when`(orderDBPort)
            .save(anyOrder())

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    orderId = orderId,
                    offerId = offer.offerId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order already belongs to another Offer")

        verify(currentUserPort).getUserId()
        verify(orderDBPort, times(2)).findById(orderId)
        verify(clock).instant()
        verify(offerQueryPort).getOffer(offer.offerId)
        verify(orderDBPort).save(anyOrder())
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            offerQueryPort,
            currentUserPort,
            clock,
        )
    }

    @Test
    fun whenSaveFailsWithDataIntegrityViolationAndOrderStillDoesNotExist_rethrowsException() {
        // Arrange
        val orderId = OrderId(UUID.randomUUID())
        val offer = createOfferSnapshot()
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val saveException = DataIntegrityViolationException("duplicate order")

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(orderDBPort.findById(orderId)).thenReturn(null)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerQueryPort.getOffer(offer.offerId)).thenReturn(offer)
        doThrow(saveException).`when`(orderDBPort).save(anyOrder())

        // Act
        val exception =
            assertThrows<DataIntegrityViolationException> {
                useCase.execute(
                    orderId = orderId,
                    offerId = offer.offerId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception).isSameAs(saveException)

        verify(currentUserPort).getUserId()
        verify(orderDBPort, times(2)).findById(orderId)
        verify(clock).instant()
        verify(offerQueryPort).getOffer(offer.offerId)
        verify(orderDBPort).save(anyOrder())
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            offerQueryPort,
            currentUserPort,
            clock,
        )
    }

    private fun createOfferSnapshot(
        offerId: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        status: OfferStatus = OfferStatus.ACTIVE,
        unitPrice: Long = 500,
        availableQuantity: Int = 5,
        pickupStart: Instant = Instant.parse("2026-08-20T12:00:00Z"),
        pickupEnd: Instant = Instant.parse("2026-08-20T14:00:00Z"),
    ): OfferSnapshot =
        OfferSnapshot(
            offerId = offerId,
            storeId = storeId,
            status = status,
            unitPrice = unitPrice,
            availableQuantity = availableQuantity,
            pickupStart = pickupStart,
            pickupEnd = pickupEnd,
        )

    private fun createOrder(
        id: OrderId = OrderId(UUID.randomUUID()),
        customerId: String = CURRENT_USER_ID,
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

    private fun anyOrder(): Order = any(Order::class.java) ?: createOrder()

    companion object {
        private const val CURRENT_USER_ID = "33333333-3333-3333-3333-333333333333"
        private const val OTHER_USER_ID = "88888888-8888-8888-8888-888888888888"
    }
}
