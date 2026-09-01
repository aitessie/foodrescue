package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.OfferReservationNotFoundException
import com.example.foodrescue.offerservice.application.ports.CurrentUserPort
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.application.ports.OfferReservationDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
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
class ReleaseFoodBagReservationUseCaseTest {
    @Mock private lateinit var offerDBPort: OfferDBPort

    @Mock private lateinit var reservationDBPort: OfferReservationDBPort

    @Mock private lateinit var currentUserPort: CurrentUserPort

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: ReleaseFoodBagReservationUseCase

    @Test
    fun whenCustomerReleasesOwnReservation_returnsSavedReservation() {
        // Arrange
        val reservation = createReservation()
        val offer =
            createOffer(
                id = reservation.offerId,
                availableQuantity = 3,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedOffer =
            createOffer(
                id = offer.id,
                storeId = offer.storeId,
                foodBagId = offer.foodBagId,
                updatedAt = now,
                version = 1,
            )
        val savedReservation =
            createReservation(
                id = reservation.id,
                offerId = reservation.offerId,
                status = ReservationStatus.RELEASED,
                createdAt = reservation.createdAt,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .offerReservationReleased(
                    offer = savedOffer,
                    reservation = savedReservation,
                    occurredAt = now,
                )

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(offerDBPort.findById(reservation.offerId)).thenReturn(offer)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerDBPort.save(offer)).thenReturn(savedOffer)
        `when`(reservationDBPort.save(reservation)).thenReturn(savedReservation)
        `when`(
                eventFactory.offerReservationReleased(
                    offer = savedOffer,
                    reservation = savedReservation,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result = useCase.execute(reservationId = reservation.id)

        // Assert
        assertThat(result).isSameAs(savedReservation)
        assertThat(offer.availableQuantity).isEqualTo(5)
        assertThat(offer.reservedQuantity).isZero()
        assertThat(offer.updatedAt).isEqualTo(now)
        assertThat(reservation.status).isEqualTo(ReservationStatus.RELEASED)
        assertThat(reservation.updatedAt).isEqualTo(now)

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(reservationDBPort).findById(reservation.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(offerDBPort).findById(reservation.offerId)
        verify(clock).instant()
        verify(offerDBPort).save(offer)
        verify(reservationDBPort).save(reservation)
        verify(eventFactory)
            .offerReservationReleased(
                offer = savedOffer,
                reservation = savedReservation,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            currentUserPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenAdminReleasesForeignReservation_returnsSavedReservation() {
        // Arrange
        val reservation = createReservation(customerId = OTHER_USER_ID)
        val offer =
            createOffer(
                id = reservation.offerId,
                availableQuantity = 3,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedOffer =
            createOffer(
                id = offer.id,
                storeId = offer.storeId,
                foodBagId = offer.foodBagId,
                updatedAt = now,
                version = 1,
            )
        val savedReservation =
            createReservation(
                id = reservation.id,
                offerId = reservation.offerId,
                customerId = OTHER_USER_ID,
                status = ReservationStatus.RELEASED,
                createdAt = reservation.createdAt,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .offerReservationReleased(
                    offer = savedOffer,
                    reservation = savedReservation,
                    occurredAt = now,
                )

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(true)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(offerDBPort.findById(reservation.offerId)).thenReturn(offer)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerDBPort.save(offer)).thenReturn(savedOffer)
        `when`(reservationDBPort.save(reservation)).thenReturn(savedReservation)
        `when`(
                eventFactory.offerReservationReleased(
                    offer = savedOffer,
                    reservation = savedReservation,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result = useCase.execute(reservationId = reservation.id)

        // Assert
        assertThat(result).isSameAs(savedReservation)
        assertThat(offer.availableQuantity).isEqualTo(5)
        assertThat(offer.reservedQuantity).isZero()
        assertThat(reservation.status).isEqualTo(ReservationStatus.RELEASED)
        assertThat(reservation.updatedAt).isEqualTo(now)

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, times(2)).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).getUserId()
        verify(reservationDBPort).findById(reservation.id)
        verify(offerDBPort).findById(reservation.offerId)
        verify(clock).instant()
        verify(offerDBPort).save(offer)
        verify(reservationDBPort).save(reservation)
        verify(eventFactory)
            .offerReservationReleased(
                offer = savedOffer,
                reservation = savedReservation,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            currentUserPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenReleasedReservationIsReleasedAgain_returnsExistingReservation() {
        // Arrange
        val reservation = createReservation(status = ReservationStatus.RELEASED)

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)

        // Act
        val result = useCase.execute(reservationId = reservation.id)

        // Assert
        assertThat(result).isSameAs(reservation)

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(reservationDBPort).findById(reservation.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verifyNoInteractions(
            offerDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            reservationDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenReservationDoesNotExist_throwsOfferReservationNotFoundException() {
        // Arrange
        val reservationId = ReservationId(UUID.randomUUID())

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferReservationNotFoundException> {
                useCase.execute(reservationId = reservationId)
            }

        // Assert
        assertThat(exception.message).contains(reservationId.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).getUserId()
        verify(reservationDBPort).findById(reservationId)
        verifyNoInteractions(
            offerDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            reservationDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenCustomerReleasesForeignReservation_throwsOfferReservationNotFoundException() {
        // Arrange
        val reservation = createReservation(customerId = OTHER_USER_ID)

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)

        // Act
        val exception =
            assertThrows<OfferReservationNotFoundException> {
                useCase.execute(reservationId = reservation.id)
            }

        // Assert
        assertThat(exception.message).contains(reservation.id.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(reservationDBPort).findById(reservation.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verifyNoInteractions(
            offerDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            reservationDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenOfferDoesNotExist_throwsOfferNotFoundException() {
        // Arrange
        val reservation = createReservation()

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(offerDBPort.findById(reservation.offerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(reservationId = reservation.id)
            }

        // Assert
        assertThat(exception.message).contains(reservation.offerId.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(reservationDBPort).findById(reservation.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(offerDBPort).findById(reservation.offerId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenReservedQuantityIsInsufficient_throwsInvalidStateException() {
        // Arrange
        val reservation = createReservation()
        val offer =
            createOffer(
                id = reservation.offerId,
                availableQuantity = 4,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(offerDBPort.findById(reservation.offerId)).thenReturn(offer)
        `when`(clock.instant()).thenReturn(now)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(reservationId = reservation.id)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Released quantity exceeds reserved Offer quantity")
        assertThat(offer.availableQuantity).isEqualTo(4)
        assertThat(reservation.status).isEqualTo(ReservationStatus.RESERVED)

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(reservationDBPort).findById(reservation.id)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(offerDBPort).findById(reservation.offerId)
        verify(clock).instant()
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            currentUserPort,
            clock,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = ApplicationRole::class,
        names =
            [
                "STAFF",
                "MANAGER",
            ],
    )
    fun whenManagementRoleReleasesReservation_throwsAccessDeniedException(role: ApplicationRole) {
        // Arrange
        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER))
            .thenReturn(role == ApplicationRole.CUSTOMER)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN))
            .thenReturn(role == ApplicationRole.ADMIN)

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                useCase.execute(reservationId = ReservationId(UUID.randomUUID()))
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot release OfferReservation")

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).getUserId()
        verifyNoInteractions(
            offerDBPort,
            reservationDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(currentUserPort)
    }

    @Test
    fun whenUserHasNoApplicationRole_throwsAccessDeniedException() {
        // Arrange
        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                useCase.execute(reservationId = ReservationId(UUID.randomUUID()))
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot release OfferReservation")

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).getUserId()
        verifyNoInteractions(
            offerDBPort,
            reservationDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(currentUserPort)
    }

    private fun createOffer(
        id: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        foodBagId: FoodBagId = FoodBagId(UUID.randomUUID()),
        availableQuantity: Int = 5,
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
    ): Offer =
        Offer(
            id = id,
            storeId = storeId,
            foodBagId = foodBagId,
            category = FoodBagCategory.entries.first(),
            unitPrice =
                Money(
                    amountMinor = 500,
                    currency = MoneyCurrency.RUB,
                ),
            allergens = emptySet(),
            status = OfferStatus.ACTIVE,
            totalQuantity = 5,
            availableQuantity = availableQuantity,
            pickupWindow =
                PickupWindow(
                    start = Instant.parse("2026-08-20T12:00:00Z"),
                    end = Instant.parse("2026-08-20T14:00:00Z"),
                ),
            createdAt = Instant.parse("2026-08-20T10:00:00Z"),
            updatedAt = updatedAt,
            version = version,
        )

    private fun createReservation(
        id: ReservationId = ReservationId(UUID.randomUUID()),
        offerId: OfferId = OfferId(UUID.randomUUID()),
        customerId: String = CURRENT_USER_ID,
        quantity: Int = 2,
        status: ReservationStatus = ReservationStatus.RESERVED,
        createdAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
    ): OfferReservation =
        OfferReservation(
            id = id,
            offerId = offerId,
            customerId = customerId,
            quantity = quantity,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    companion object {
        private const val CURRENT_USER_ID = "33333333-3333-3333-3333-333333333333"
        private const val OTHER_USER_ID = "88888888-8888-8888-8888-888888888888"
    }
}
