package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.access.OrderAccessPolicy
import com.example.foodrescue.orderservice.application.exceptions.OrderAccessDeniedException
import com.example.foodrescue.orderservice.application.exceptions.OrderConflictException
import com.example.foodrescue.orderservice.application.exceptions.OrderNotFoundException
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.application.ports.PickupTokenDBPort
import com.example.foodrescue.orderservice.application.ports.PickupTokenGeneratorPort
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
class GetPickupTokenUseCaseTest {
    @Mock private lateinit var orderDBPort: OrderDBPort

    @Mock private lateinit var pickupTokenDBPort: PickupTokenDBPort

    @Mock private lateinit var pickupTokenGeneratorPort: PickupTokenGeneratorPort

    @Mock private lateinit var pickupTokenHashPort: PickupTokenHashPort

    @Mock private lateinit var orderAccessPolicy: OrderAccessPolicy

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: GetPickupTokenUseCase

    @Test
    fun whenReservedOrderHasNoPickupToken_createsAndReturnsPickupToken() {
        // Arrange
        val order = createOrder()
        val now = Instant.parse("2026-08-20T11:00:00Z")
        var tokenToSave: PickupToken? = null

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(pickupTokenDBPort.findByOrderId(order.id)).thenReturn(null)
        `when`(pickupTokenGeneratorPort.generate()).thenReturn(RAW_TOKEN)
        `when`(clock.instant()).thenReturn(now)
        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        doAnswer { invocation ->
            tokenToSave = invocation.getArgument(0)
            invocation.getArgument<PickupToken>(0)
        }
            .`when`(pickupTokenDBPort)
            .save(anyPickupToken())

        // Act
        val result = useCase.execute(orderId = order.id)

        // Assert
        assertThat(result).isEqualTo(RAW_TOKEN)

        assertThat(tokenToSave).isNotNull
        assertThat(tokenToSave!!.orderId).isEqualTo(order.id)
        assertThat(tokenToSave!!.tokenHash).isEqualTo(TOKEN_HASH)
        assertThat(tokenToSave!!.usedAt).isNull()
        assertThat(tokenToSave!!.version).isZero()
        assertThat(tokenToSave!!.createdAt).isEqualTo(now)
        assertThat(tokenToSave!!.updatedAt).isEqualTo(now)

        verify(orderDBPort).findById(order.id)
        verify(orderAccessPolicy).checkReadAccess(order)
        verify(pickupTokenDBPort).findByOrderId(order.id)
        verify(pickupTokenGeneratorPort).generate()
        verify(clock).instant()
        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).save(tokenToSave!!)
        verifyNoMoreInteractions(
            orderDBPort,
            pickupTokenDBPort,
            pickupTokenGeneratorPort,
            pickupTokenHashPort,
            orderAccessPolicy,
            clock,
        )
    }

    @Test
    fun whenReservedOrderHasUnusedPickupToken_reissuesTokenPreservingVersionAndCreatedAt() {
        // Arrange
        val order = createOrder()
        val existingToken =
            createPickupToken(
                orderId = order.id,
                tokenHash = OLD_TOKEN_HASH,
                version = 3,
                createdAt = Instant.parse("2026-08-20T10:30:00Z"),
                updatedAt = Instant.parse("2026-08-20T10:30:00Z"),
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        var tokenToSave: PickupToken? = null

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(pickupTokenDBPort.findByOrderId(order.id)).thenReturn(existingToken)
        `when`(pickupTokenGeneratorPort.generate()).thenReturn(RAW_TOKEN)
        `when`(clock.instant()).thenReturn(now)
        `when`(pickupTokenHashPort.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH)
        doAnswer { invocation ->
            tokenToSave = invocation.getArgument(0)
            invocation.getArgument<PickupToken>(0)
        }
            .`when`(pickupTokenDBPort)
            .save(anyPickupToken())

        // Act
        val result = useCase.execute(orderId = order.id)

        // Assert
        assertThat(result).isEqualTo(RAW_TOKEN)

        assertThat(tokenToSave).isNotNull
        assertThat(tokenToSave!!.orderId).isEqualTo(order.id)
        assertThat(tokenToSave!!.tokenHash).isEqualTo(TOKEN_HASH)
        assertThat(tokenToSave!!.usedAt).isNull()
        assertThat(tokenToSave!!.version).isEqualTo(existingToken.version)
        assertThat(tokenToSave!!.createdAt).isEqualTo(existingToken.createdAt)
        assertThat(tokenToSave!!.updatedAt).isEqualTo(now)

        verify(orderDBPort).findById(order.id)
        verify(orderAccessPolicy).checkReadAccess(order)
        verify(pickupTokenDBPort).findByOrderId(order.id)
        verify(pickupTokenGeneratorPort).generate()
        verify(clock).instant()
        verify(pickupTokenHashPort).hash(RAW_TOKEN)
        verify(pickupTokenDBPort).save(tokenToSave!!)
        verifyNoMoreInteractions(
            orderDBPort,
            pickupTokenDBPort,
            pickupTokenGeneratorPort,
            pickupTokenHashPort,
            orderAccessPolicy,
            clock,
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
            pickupTokenDBPort,
            pickupTokenGeneratorPort,
            pickupTokenHashPort,
            orderAccessPolicy,
            clock,
        )
        verifyNoMoreInteractions(orderDBPort)
    }

    @Test
    fun whenUserHasNoReadAccess_throwsOrderAccessDeniedException() {
        // Arrange
        val order = createOrder()

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        doThrow(OrderAccessDeniedException())
            .`when`(orderAccessPolicy)
            .checkReadAccess(order)

        // Act
        val exception =
            assertThrows<OrderAccessDeniedException> {
                useCase.execute(orderId = order.id)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user has no access to this Order")

        verify(orderDBPort).findById(order.id)
        verify(orderAccessPolicy).checkReadAccess(order)
        verifyNoInteractions(
            pickupTokenDBPort,
            pickupTokenGeneratorPort,
            pickupTokenHashPort,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            orderAccessPolicy,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["RESERVED"],
    )
    fun whenOrderIsNotReserved_throwsOrderConflictException(status: OrderStatus) {
        // Arrange
        val order = createOrder(status = status)

        `when`(orderDBPort.findById(order.id)).thenReturn(order)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(orderId = order.id)
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Pickup token is available only for a reserved Order")

        verify(orderDBPort).findById(order.id)
        verify(orderAccessPolicy).checkReadAccess(order)
        verifyNoInteractions(
            pickupTokenDBPort,
            pickupTokenGeneratorPort,
            pickupTokenHashPort,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            orderAccessPolicy,
        )
    }

    @Test
    fun whenPickupTokenHasAlreadyBeenUsed_throwsOrderConflictException() {
        // Arrange
        val order = createOrder()
        val existingToken =
            createPickupToken(
                orderId = order.id,
                usedAt = Instant.parse("2026-08-20T10:30:00Z"),
            )

        `when`(orderDBPort.findById(order.id)).thenReturn(order)
        `when`(pickupTokenDBPort.findByOrderId(order.id)).thenReturn(existingToken)

        // Act
        val exception =
            assertThrows<OrderConflictException> {
                useCase.execute(orderId = order.id)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Pickup token has already been used")

        verify(orderDBPort).findById(order.id)
        verify(orderAccessPolicy).checkReadAccess(order)
        verify(pickupTokenDBPort).findByOrderId(order.id)
        verifyNoInteractions(
            pickupTokenGeneratorPort,
            pickupTokenHashPort,
            clock,
        )
        verifyNoMoreInteractions(
            orderDBPort,
            pickupTokenDBPort,
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
        tokenHash: String = OLD_TOKEN_HASH,
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
        private const val OLD_TOKEN_HASH = "old-pickup-token-hash"
    }
}
