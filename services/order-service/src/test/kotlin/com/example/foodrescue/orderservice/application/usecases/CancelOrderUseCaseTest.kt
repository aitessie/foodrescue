package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.access.PartnerStoreAccessPolicy
import com.example.foodrescue.orderservice.application.events.ApplicationEventFactory
import com.example.foodrescue.orderservice.application.exceptions.OrderAccessDeniedException
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.ports.CurrentUserPort
import com.example.foodrescue.orderservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PaymentCommandPort
import com.example.foodrescue.orderservice.configuration.OrderCancellationProperties
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.StoreId
import com.example.foodrescue.orderservice.domain.enum.ApplicationRole
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Clock
import java.time.Duration
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
class CancelOrderUseCaseTest {
    @Mock private lateinit var orderDBPort: OrderDBPort

    @Mock private lateinit var currentUserPort: CurrentUserPort

    @Mock private lateinit var partnerStoreAccessPolicy: PartnerStoreAccessPolicy

    @Mock private lateinit var paymentCommandPort: PaymentCommandPort

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var properties: OrderCancellationProperties

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: CancelOrderUseCase

    @Test
    fun whenCustomerCancelsOwnPendingOrderBeforeDeadline_returnsSavedOrder() {
        // Arrange
        val order = createOrder()
        val now = Instant.parse("2026-08-20T11:00:00Z")
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
                status = OrderStatus.CANCELLED,
                createdAt = order.createdAt,
                updatedAt = now,
                version = 1,
            )

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(clock.instant()).thenReturn(now)
        `when`(properties.customerDeadlineBeforePickupStart).thenReturn(Duration.ofMinutes(30))
        `when`(orderDBPort.save(order)).thenReturn(savedOrder)

        // Act
        val result = useCase.execute(orderId = order.id)

        // Assert
        assertThat(result).isSameAs(savedOrder)
        assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).getUserId()
        verify(clock).instant()
        verify(properties).customerDeadlineBeforePickupStart
        verify(orderDBPort).save(order)

        verifyNoInteractions(
            partnerStoreAccessPolicy,
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
            properties,
            clock,
        )
    }

    @Test
    fun whenAdminCancelsReservedOrder_returnsSavedOrderAndRequestsReservationReleaseAndPaymentVoid() {
        // Arrange
        val order = createOrder(status = OrderStatus.RESERVED)
        val now = Instant.parse("2026-08-20T11:00:00Z")
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
                status = OrderStatus.CANCELLED,
                createdAt = order.createdAt,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .orderReservationReleaseRequested(
                    order = savedOrder,
                    occurredAt = now,
                )

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(true)
        `when`(clock.instant()).thenReturn(now)
        `when`(orderDBPort.save(order)).thenReturn(savedOrder)
        `when`(
                eventFactory.orderReservationReleaseRequested(
                    order = savedOrder,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result = useCase.execute(orderId = order.id)

        // Assert
        assertThat(result).isSameAs(savedOrder)
        assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort, never()).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).getUserId()
        verify(clock).instant()
        verify(orderDBPort).save(order)
        verify(eventFactory)
            .orderReservationReleaseRequested(
                order = savedOrder,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verify(paymentCommandPort)
            .requestVoid(
                orderId = savedOrder.id,
                amount = savedOrder.totalAmount,
            )

        verifyNoInteractions(
            partnerStoreAccessPolicy,
            properties,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenManagerCancelsPendingOrder_returnsSavedOrder() {
        // Arrange
        val order = createOrder()
        val now = Instant.parse("2026-08-20T11:00:00Z")
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
                status = OrderStatus.CANCELLED,
                createdAt = order.createdAt,
                updatedAt = now,
                version = 1,
            )

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(true)
        `when`(clock.instant()).thenReturn(now)
        `when`(orderDBPort.save(order)).thenReturn(savedOrder)

        // Act
        val result = useCase.execute(orderId = order.id)

        // Assert
        assertThat(result).isSameAs(savedOrder)
        assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort, never()).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).getUserId()
        verify(partnerStoreAccessPolicy).checkCancellationAccess(order.storeId)
        verify(clock).instant()
        verify(orderDBPort).save(order)

        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
            properties,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
            partnerStoreAccessPolicy,
            clock,
        )
    }

    @Test
    fun whenStaffCancelsPendingOrder_returnsSavedOrder() {
        // Arrange
        val order = createOrder()
        val now = Instant.parse("2026-08-20T11:00:00Z")
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
                status = OrderStatus.CANCELLED,
                createdAt = order.createdAt,
                updatedAt = now,
                version = 1,
            )

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(true)
        `when`(clock.instant()).thenReturn(now)
        `when`(orderDBPort.save(order)).thenReturn(savedOrder)

        // Act
        val result = useCase.execute(orderId = order.id)

        // Assert
        assertThat(result).isSameAs(savedOrder)
        assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort, never()).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).getUserId()
        verify(partnerStoreAccessPolicy).checkCancellationAccess(order.storeId)
        verify(clock).instant()
        verify(orderDBPort).save(order)

        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
            properties,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
            partnerStoreAccessPolicy,
            clock,
        )
    }

    @Test
    fun whenCancelledOrderIsCancelledAgain_returnsExistingOrder() {
        // Arrange
        val order = createOrder(status = OrderStatus.CANCELLED)

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(true)

        // Act
        val result = useCase.execute(orderId = order.id)

        // Assert
        assertThat(result).isSameAs(order)

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort, never()).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).getUserId()

        verifyNoInteractions(
            partnerStoreAccessPolicy,
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
            properties,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
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
        verifyNoInteractions(
            currentUserPort,
            partnerStoreAccessPolicy,
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
            properties,
            clock,
        )
        verifyNoMoreInteractions(orderDBPort)
    }

    @Test
    fun whenCustomerCancelsForeignOrder_throwsOrderAccessDeniedException() {
        // Arrange
        val order = createOrder(customerId = OTHER_USER_ID)

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)

        // Act
        val exception =
            assertThrows<OrderAccessDeniedException> {
                useCase.execute(orderId = order.id)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user has no access to this Order")

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).getUserId()

        verifyNoInteractions(
            partnerStoreAccessPolicy,
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
            properties,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenUserHasNoApplicationRole_throwsOrderAccessDeniedException() {
        // Arrange
        val order = createOrder()

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(false)

        // Act
        val exception =
            assertThrows<OrderAccessDeniedException> {
                useCase.execute(orderId = order.id)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user has no access to this Order")

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).getUserId()

        verifyNoInteractions(
            partnerStoreAccessPolicy,
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
            properties,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenStoreUserHasNoCancellationAccess_throwsOrderAccessDeniedException() {
        // Arrange
        val order = createOrder()

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(true)
        doThrow(OrderAccessDeniedException())
            .`when`(partnerStoreAccessPolicy)
            .checkCancellationAccess(order.storeId)

        // Act
        val exception =
            assertThrows<OrderAccessDeniedException> {
                useCase.execute(orderId = order.id)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user has no access to this Order")

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort, never()).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).getUserId()
        verify(partnerStoreAccessPolicy).checkCancellationAccess(order.storeId)

        verifyNoInteractions(
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
            properties,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
            partnerStoreAccessPolicy,
        )
    }

    @Test
    fun whenCustomerCancelsOrderAtCancellationDeadline_throwsOrderConflictException() {
        // Arrange
        val order = createOrder()
        val deadlineBeforePickupStart = Duration.ofMinutes(30)
        val now = order.pickupStart.minus(deadlineBeforePickupStart)

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(clock.instant()).thenReturn(now)
        `when`(properties.customerDeadlineBeforePickupStart).thenReturn(deadlineBeforePickupStart)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(orderId = order.id)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Customer cancellation deadline has passed")
        assertThat(order.status).isEqualTo(OrderStatus.PENDING)
        assertThat(order.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).getUserId()
        verify(clock).instant()
        verify(properties).customerDeadlineBeforePickupStart

        verifyNoInteractions(
            partnerStoreAccessPolicy,
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
            properties,
            clock,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        names =
            [
                "PICKED_UP",
                "COMPLETED",
                "FAILED",
                "NO_SHOW",
            ],
    )
    fun whenOrderStatusIsNotCancellable_throwsOrderConflictException(status: OrderStatus) {
        // Arrange
        val order = createOrder(status = status)

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(true)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(orderId = order.id)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order in status $status cannot be cancelled")
        assertThat(order.status).isEqualTo(status)

        verify(orderDBPort).findById(order.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verify(currentUserPort, never()).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).getUserId()

        verifyNoInteractions(
            partnerStoreAccessPolicy,
            paymentCommandPort,
            eventFactory,
            eventPublisherPort,
            properties,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
        )
    }

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

    companion object {
        private const val CURRENT_USER_ID = "33333333-3333-3333-3333-333333333333"

        private const val OTHER_USER_ID = "88888888-8888-8888-8888-888888888888"
    }
}
