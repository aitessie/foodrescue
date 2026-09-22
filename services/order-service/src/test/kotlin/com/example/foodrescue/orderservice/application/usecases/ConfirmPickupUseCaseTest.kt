package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.access.PartnerStoreAccessPolicy
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.exceptions.PickupAccessDeniedException
import com.example.foodrescue.orderservice.application.exceptions.PickupTokenNotFoundException
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PaymentCommandPort
import com.example.foodrescue.orderservice.application.ports.PickupTokenDBPort
import com.example.foodrescue.orderservice.application.ports.PickupTokenHashPort
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.PickupToken
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
class ConfirmPickupUseCaseTest {
    @Mock private lateinit var pickupTokenDBPort: PickupTokenDBPort

    @Mock private lateinit var pickupTokenHashPort: PickupTokenHashPort

    @Mock private lateinit var orderDBPort: OrderDBPort

    @Mock private lateinit var partnerStoreAccessPolicy: PartnerStoreAccessPolicy

    @Mock private lateinit var paymentCommandPort: PaymentCommandPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: ConfirmPickupUseCase

    @Test
    fun whenReservedOrderIsPickedUpDuringPickupWindow_returnsReloadedOrder() {
        // Arrange
        val order = createOrder()
        val pickupToken = createPickupToken(orderId = order.id)
        val now = Instant.parse("2026-08-20T13:00:00Z")
        val pickedUpOrder =
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
                status = OrderStatus.PICKED_UP,
                createdAt = order.createdAt,
                updatedAt = now,
                version = 1,
            )
        val reloadedOrder =
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
                status = OrderStatus.PICKED_UP,
                createdAt = order.createdAt,
                updatedAt = now,
                version = 1,
            )
        var pickupTokenToSave: PickupToken? = null

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(order.id))
            .thenReturn(order)
            .thenReturn(reloadedOrder)
        `when`(clock.instant()).thenReturn(now)
        doAnswer { invocation ->
            pickupTokenToSave = invocation.getArgument(0)
            invocation.getArgument<PickupToken>(0)
        }
            .`when`(pickupTokenDBPort)
            .save(anyPickupToken())
        `when`(orderDBPort.save(order)).thenReturn(pickedUpOrder)

        // Act
        val result =
            useCase.execute(
                storeId = order.storeId,
                rawToken = RAW_TOKEN,
            )

        // Assert
        assertThat(result).isSameAs(reloadedOrder)

        assertThat(pickupTokenToSave).isNotNull
        assertThat(pickupTokenToSave!!.orderId).isEqualTo(pickupToken.orderId)
        assertThat(pickupTokenToSave!!.tokenHash).isEqualTo(pickupToken.tokenHash)
        assertThat(pickupTokenToSave!!.usedAt).isEqualTo(now)
        assertThat(pickupTokenToSave!!.version).isEqualTo(pickupToken.version)
        assertThat(pickupTokenToSave!!.createdAt).isEqualTo(pickupToken.createdAt)
        assertThat(pickupTokenToSave!!.updatedAt).isEqualTo(now)

        assertThat(order.status).isEqualTo(OrderStatus.PICKED_UP)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort, times(2)).findById(order.id)
        verify(partnerStoreAccessPolicy).checkAccess(order.storeId)
        verify(clock).instant()
        verify(pickupTokenDBPort).save(anyPickupToken())
        verify(orderDBPort).save(order)
        verify(paymentCommandPort)
            .requestCapture(
                orderId = pickedUpOrder.id,
                amount = pickedUpOrder.totalAmount,
            )
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
            partnerStoreAccessPolicy,
            paymentCommandPort,
            clock,
        )
    }

    @Test
    fun whenRawTokenIsBlank_throwsPickupTokenNotFoundException() {
        // Arrange
        val storeId = StoreId(UUID.randomUUID())

        // Act
        val exception =
            assertThrows<PickupTokenNotFoundException> {
                useCase.execute(
                    storeId = storeId,
                    rawToken = " ",
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Pickup token not found")

        verifyNoInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
            partnerStoreAccessPolicy,
            paymentCommandPort,
            clock,
        )
    }

    @Test
    fun whenPickupTokenDoesNotExist_throwsPickupTokenNotFoundException() {
        // Arrange
        val storeId = StoreId(UUID.randomUUID())

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(null)

        // Act
        val exception =
            assertThrows<PickupTokenNotFoundException> {
                useCase.execute(
                    storeId = storeId,
                    rawToken = RAW_TOKEN,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Pickup token not found")

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verifyNoInteractions(
            orderDBPort,
            partnerStoreAccessPolicy,
            paymentCommandPort,
            clock,
        )
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
        )
    }

    @Test
    fun whenPickupTokenOrderDoesNotExist_throwsOrderNotFoundException() {
        // Arrange
        val storeId = StoreId(UUID.randomUUID())
        val pickupToken = createPickupToken()

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(pickupToken.orderId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OrderNotFoundException> {
                useCase.execute(
                    storeId = storeId,
                    rawToken = RAW_TOKEN,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order not found: ${pickupToken.orderId.value}")

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort).findById(pickupToken.orderId)
        verifyNoInteractions(
            partnerStoreAccessPolicy,
            paymentCommandPort,
            clock,
        )
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
        )
    }

    @Test
    fun whenOrderBelongsToAnotherStore_throwsPickupTokenNotFoundException() {
        // Arrange
        val requestedStoreId = StoreId(UUID.randomUUID())
        val order = createOrder()
        val pickupToken = createPickupToken(orderId = order.id)

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<PickupTokenNotFoundException> {
                useCase.execute(
                    storeId = requestedStoreId,
                    rawToken = RAW_TOKEN,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Pickup token not found")

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort).findById(order.id)
        verifyNoInteractions(
            partnerStoreAccessPolicy,
            paymentCommandPort,
            clock,
        )
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
        )
    }

    @Test
    fun whenUserCannotConfirmPickupForStore_throwsPickupAccessDeniedException() {
        // Arrange
        val order = createOrder()
        val pickupToken = createPickupToken(orderId = order.id)

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        doThrow(PickupAccessDeniedException())
            .`when`(partnerStoreAccessPolicy)
            .checkAccess(order.storeId)

        // Act
        val exception =
            assertThrows<PickupAccessDeniedException> {
                useCase.execute(
                    storeId = order.storeId,
                    rawToken = RAW_TOKEN,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot confirm pickup for this Store")

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort).findById(order.id)
        verify(partnerStoreAccessPolicy).checkAccess(order.storeId)
        verifyNoInteractions(
            paymentCommandPort,
            clock,
        )
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
            partnerStoreAccessPolicy,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        names = ["PICKED_UP", "COMPLETED"],
    )
    fun whenUsedPickupTokenIsUsedForAlreadyPickedUpOrder_returnsExistingOrder(
        status: OrderStatus
    ) {
        // Arrange
        val order = createOrder(status = status)
        val pickupToken =
            createPickupToken(
                orderId = order.id,
                usedAt = Instant.parse("2026-08-20T13:00:00Z"),
            )

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val result =
            useCase.execute(
                storeId = order.storeId,
                rawToken = RAW_TOKEN,
            )

        // Assert
        assertThat(result).isSameAs(order)

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort).findById(order.id)
        verify(partnerStoreAccessPolicy).checkAccess(order.storeId)
        verifyNoInteractions(
            paymentCommandPort,
            clock,
        )
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
            partnerStoreAccessPolicy,
        )
    }

    @Test
    fun whenUsedPickupTokenIsUsedForOrderThatIsNotPickedUp_throwsOrderConflictException() {
        // Arrange
        val order = createOrder(status = OrderStatus.RESERVED)
        val pickupToken =
            createPickupToken(
                orderId = order.id,
                usedAt = Instant.parse("2026-08-20T13:00:00Z"),
            )

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    storeId = order.storeId,
                    rawToken = RAW_TOKEN,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Pickup token has already been used")

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort).findById(order.id)
        verify(partnerStoreAccessPolicy).checkAccess(order.storeId)
        verifyNoInteractions(
            paymentCommandPort,
            clock,
        )
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
            partnerStoreAccessPolicy,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        names =
            [
                "PENDING",
                "PICKED_UP",
                "COMPLETED",
                "FAILED",
                "CANCELLED",
                "NO_SHOW",
            ],
    )
    fun whenOrderIsNotReserved_throwsOrderConflictException(status: OrderStatus) {
        // Arrange
        val order = createOrder(status = status)
        val pickupToken = createPickupToken(orderId = order.id)

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    storeId = order.storeId,
                    rawToken = RAW_TOKEN,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Pickup cannot be confirmed for Order in status $status")

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort).findById(order.id)
        verify(partnerStoreAccessPolicy).checkAccess(order.storeId)
        verifyNoInteractions(
            paymentCommandPort,
            clock,
        )
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
            partnerStoreAccessPolicy,
        )
    }

    @Test
    fun whenPickupIsConfirmedBeforePickupWindow_throwsOrderConflictException() {
        // Arrange
        val order = createOrder()
        val pickupToken = createPickupToken(orderId = order.id)
        val now = order.pickupStart.minusSeconds(1)

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(clock.instant()).thenReturn(now)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    storeId = order.storeId,
                    rawToken = RAW_TOKEN,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Pickup can be confirmed only during the Order pickup window")
        assertThat(order.status).isEqualTo(OrderStatus.RESERVED)
        assertThat(order.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort).findById(order.id)
        verify(partnerStoreAccessPolicy).checkAccess(order.storeId)
        verify(clock).instant()
        verifyNoInteractions(paymentCommandPort)
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
            partnerStoreAccessPolicy,
            clock,
        )
    }

    @Test
    fun whenPickupIsConfirmedAfterPickupWindow_throwsOrderConflictException() {
        // Arrange
        val order = createOrder()
        val pickupToken = createPickupToken(orderId = order.id)
        val now = order.pickupEnd.plusSeconds(1)

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(clock.instant()).thenReturn(now)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(
                    storeId = order.storeId,
                    rawToken = RAW_TOKEN,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Pickup can be confirmed only during the Order pickup window")
        assertThat(order.status).isEqualTo(OrderStatus.RESERVED)
        assertThat(order.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort).findById(order.id)
        verify(partnerStoreAccessPolicy).checkAccess(order.storeId)
        verify(clock).instant()
        verifyNoInteractions(paymentCommandPort)
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
            partnerStoreAccessPolicy,
            clock,
        )
    }

    @Test
    fun whenPickedUpOrderCannotBeReloaded_throwsOrderNotFoundException() {
        // Arrange
        val order = createOrder()
        val pickupToken = createPickupToken(orderId = order.id)
        val now = Instant.parse("2026-08-20T13:00:00Z")
        val pickedUpOrder =
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
                status = OrderStatus.PICKED_UP,
                createdAt = order.createdAt,
                updatedAt = now,
                version = 1,
            )

        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        `when`(pickupTokenDBPort.findByTokenHash(TOKEN_HASH)).thenReturn(pickupToken)
        `when`(orderDBPort.findById(order.id))
            .thenReturn(order)
            .thenReturn(null)
        `when`(clock.instant()).thenReturn(now)
        doAnswer { invocation -> invocation.getArgument<PickupToken>(0) }
            .`when`(pickupTokenDBPort)
            .save(anyPickupToken())
        `when`(orderDBPort.save(order)).thenReturn(pickedUpOrder)

        // Act
        val exception =
            assertThrows<OrderNotFoundException> {
                useCase.execute(
                    storeId = order.storeId,
                    rawToken = RAW_TOKEN,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Order not found: ${pickedUpOrder.id.value}")
        assertThat(order.status).isEqualTo(OrderStatus.PICKED_UP)
        assertThat(order.updatedAt).isEqualTo(now)

        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).findByTokenHash(TOKEN_HASH)
        verify(orderDBPort, times(2)).findById(order.id)
        verify(partnerStoreAccessPolicy).checkAccess(order.storeId)
        verify(clock).instant()
        verify(pickupTokenDBPort).save(anyPickupToken())
        verify(orderDBPort).save(order)
        verify(paymentCommandPort)
            .requestCapture(
                orderId = pickedUpOrder.id,
                amount = pickedUpOrder.totalAmount,
            )
        verifyNoMoreInteractions(
            pickupTokenDBPort,
            pickupTokenHashPort,
            orderDBPort,
            partnerStoreAccessPolicy,
            paymentCommandPort,
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

    private fun createPickupToken(
        orderId: OrderId = OrderId(UUID.randomUUID()),
        tokenHash: String = TOKEN_HASH,
        usedAt: Instant? = null,
        version: Long = 0,
        createdAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
    ): PickupToken =
        PickupToken(
            orderId = orderId,
            tokenHash = tokenHash,
            usedAt = usedAt,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun anyPickupToken(): PickupToken =
        any(PickupToken::class.java) ?: createPickupToken()

    companion object {
        private const val CUSTOMER_ID = "33333333-3333-3333-3333-333333333333"
        private const val RAW_TOKEN = "pickup-token"
        private const val TOKEN_HASH = "pickup-token-hash"
    }
}
