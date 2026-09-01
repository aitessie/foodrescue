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
import com.example.foodrescue.offerservice.application.ports.StoreSnapshotDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.entities.StoreSnapshot
import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus
import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
import com.example.foodrescue.offerservice.domain.`enum`.StoreStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
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
class ReserveFoodBagsUseCaseTest {
    @Mock private lateinit var offerDBPort: OfferDBPort

    @Mock private lateinit var reservationDBPort: OfferReservationDBPort

    @Mock private lateinit var storeSnapshotDBPort: StoreSnapshotDBPort

    @Mock private lateinit var currentUserPort: CurrentUserPort

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: ReserveFoodBagsUseCase

    @Test
    fun whenCustomerReservesAvailableFoodBags_returnsSavedReservation() {
        // Arrange
        val offer = createOffer()
        val reservationId = ReservationId(UUID.randomUUID())
        val snapshot = createStoreSnapshot(storeId = offer.storeId)
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedOffer =
            createOffer(
                id = offer.id,
                storeId = offer.storeId,
                foodBagId = offer.foodBagId,
                availableQuantity = 3,
                updatedAt = now,
                version = 1,
            )
        val savedReservation =
            createReservation(
                id = reservationId,
                offerId = offer.id,
                createdAt = now,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .offerReserved(
                    offer = savedOffer,
                    reservation = savedReservation,
                    occurredAt = now,
                )
        var reservationToSave: OfferReservation? = null

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)
        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(storeSnapshotDBPort.findById(offer.storeId)).thenReturn(snapshot)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerDBPort.save(offer)).thenReturn(savedOffer)
        doAnswer { invocation ->
                reservationToSave = invocation.getArgument(0)
                savedReservation
            }
            .`when`(reservationDBPort)
            .save(anyReservation())
        `when`(
                eventFactory.offerReserved(
                    offer = savedOffer,
                    reservation = savedReservation,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result =
            useCase.execute(
                offerId = offer.id,
                reservationId = reservationId,
                quantity = 2,
            )

        // Assert
        val createdReservation = requireNotNull(reservationToSave)

        assertThat(result).isSameAs(savedReservation)

        assertThat(offer.totalQuantity).isEqualTo(5)
        assertThat(offer.availableQuantity).isEqualTo(3)
        assertThat(offer.reservedQuantity).isEqualTo(2)
        assertThat(offer.updatedAt).isEqualTo(now)

        assertThat(createdReservation.id).isEqualTo(reservationId)
        assertThat(createdReservation.offerId).isEqualTo(offer.id)
        assertThat(createdReservation.customerId).isEqualTo(CURRENT_USER_ID)
        assertThat(createdReservation.quantity).isEqualTo(2)
        assertThat(createdReservation.status).isEqualTo(ReservationStatus.RESERVED)
        assertThat(createdReservation.createdAt).isEqualTo(now)
        assertThat(createdReservation.updatedAt).isEqualTo(now)
        assertThat(createdReservation.version).isZero()

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservationId)
        verify(offerDBPort).findById(offer.id)
        verify(storeSnapshotDBPort).findById(offer.storeId)
        verify(clock).instant()
        verify(offerDBPort).save(offer)
        verify(reservationDBPort).save(createdReservation)
        verify(eventFactory)
            .offerReserved(
                offer = savedOffer,
                reservation = savedReservation,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            storeSnapshotDBPort,
            currentUserPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenAdminRepeatsSameReservation_returnsExistingReservation() {
        // Arrange
        val offerId = OfferId(UUID.randomUUID())
        val reservation = createReservation(offerId = offerId)

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)

        // Act
        val result =
            useCase.execute(
                offerId = offerId,
                reservationId = reservation.id,
                quantity = reservation.quantity,
            )

        // Assert
        assertThat(result).isSameAs(reservation)

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservation.id)
        verifyNoInteractions(
            offerDBPort,
            storeSnapshotDBPort,
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
    fun whenExistingReservationBelongsToAnotherCustomer_throwsOfferReservationNotFoundException() {
        // Arrange
        val offerId = OfferId(UUID.randomUUID())
        val reservation =
            createReservation(
                offerId = offerId,
                customerId = OTHER_USER_ID,
            )

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)

        // Act
        val exception =
            assertThrows<OfferReservationNotFoundException> {
                useCase.execute(
                    offerId = offerId,
                    reservationId = reservation.id,
                    quantity = reservation.quantity,
                )
            }

        // Assert
        assertThat(exception.message).contains(reservation.id.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservation.id)
        verifyNoInteractions(
            offerDBPort,
            storeSnapshotDBPort,
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
    fun whenExistingReservationBelongsToAnotherOffer_throwsInvalidStateException() {
        // Arrange
        val offerId = OfferId(UUID.randomUUID())
        val reservation = createReservation()

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    offerId = offerId,
                    reservationId = reservation.id,
                    quantity = reservation.quantity,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Reservation already belongs to another Offer")

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservation.id)
        verifyNoInteractions(
            offerDBPort,
            storeSnapshotDBPort,
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
    fun whenExistingReservationHasDifferentQuantity_throwsInvalidStateException() {
        // Arrange
        val offerId = OfferId(UUID.randomUUID())
        val reservation = createReservation(offerId = offerId)

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    offerId = offerId,
                    reservationId = reservation.id,
                    quantity = 3,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Reservation quantity does not match the existing reservation")

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservation.id)
        verifyNoInteractions(
            offerDBPort,
            storeSnapshotDBPort,
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
    fun whenExistingReservationWasReleased_throwsInvalidStateException() {
        // Arrange
        val offerId = OfferId(UUID.randomUUID())
        val reservation =
            createReservation(
                offerId = offerId,
                status = ReservationStatus.RELEASED,
            )

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    offerId = offerId,
                    reservationId = reservation.id,
                    quantity = reservation.quantity,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Released reservation cannot be reserved again")

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservation.id)
        verifyNoInteractions(
            offerDBPort,
            storeSnapshotDBPort,
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
        val offerId = OfferId(UUID.randomUUID())
        val reservationId = ReservationId(UUID.randomUUID())

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)
        `when`(offerDBPort.findById(offerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    offerId = offerId,
                    reservationId = reservationId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).contains(offerId.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservationId)
        verify(offerDBPort).findById(offerId)
        verifyNoInteractions(
            storeSnapshotDBPort,
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
    fun whenStoreSnapshotDoesNotExist_throwsOfferNotFoundException() {
        // Arrange
        val offer = createOffer()
        val reservationId = ReservationId(UUID.randomUUID())

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)
        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(storeSnapshotDBPort.findById(offer.storeId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    offerId = offer.id,
                    reservationId = reservationId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).contains(offer.id.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservationId)
        verify(offerDBPort).findById(offer.id)
        verify(storeSnapshotDBPort).findById(offer.storeId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            storeSnapshotDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenPartnerSnapshotIsNotActive_throwsOfferNotFoundException() {
        // Arrange
        val offer = createOffer()
        val reservationId = ReservationId(UUID.randomUUID())
        val snapshot =
            createStoreSnapshot(
                storeId = offer.storeId,
                partnerStatus = PartnerStatus.SUSPENDED,
            )

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)
        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(storeSnapshotDBPort.findById(offer.storeId)).thenReturn(snapshot)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    offerId = offer.id,
                    reservationId = reservationId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).contains(offer.id.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservationId)
        verify(offerDBPort).findById(offer.id)
        verify(storeSnapshotDBPort).findById(offer.storeId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            storeSnapshotDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenStoreSnapshotIsNotActive_throwsOfferNotFoundException() {
        // Arrange
        val offer = createOffer()
        val reservationId = ReservationId(UUID.randomUUID())
        val snapshot =
            createStoreSnapshot(
                storeId = offer.storeId,
                storeStatus = StoreStatus.SUSPENDED,
            )

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)
        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(storeSnapshotDBPort.findById(offer.storeId)).thenReturn(snapshot)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    offerId = offer.id,
                    reservationId = reservationId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).contains(offer.id.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservationId)
        verify(offerDBPort).findById(offer.id)
        verify(storeSnapshotDBPort).findById(offer.storeId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            storeSnapshotDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenReservationQuantityIsNotPositive_throwsInvalidStateException() {
        // Arrange
        val offer = createOffer()
        val reservationId = ReservationId(UUID.randomUUID())
        val snapshot = createStoreSnapshot(storeId = offer.storeId)
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)
        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(storeSnapshotDBPort.findById(offer.storeId)).thenReturn(snapshot)
        `when`(clock.instant()).thenReturn(now)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    offerId = offer.id,
                    reservationId = reservationId,
                    quantity = 0,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Reservation quantity must be greater than zero")
        assertThat(offer.availableQuantity).isEqualTo(5)
        assertThat(offer.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservationId)
        verify(offerDBPort).findById(offer.id)
        verify(storeSnapshotDBPort).findById(offer.storeId)
        verify(clock).instant()
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            storeSnapshotDBPort,
            currentUserPort,
            clock,
        )
    }

    @Test
    fun whenOfferIsNotActive_throwsInvalidStateException() {
        // Arrange
        val offer = createOffer(status = OfferStatus.SCHEDULED)
        val reservationId = ReservationId(UUID.randomUUID())
        val snapshot = createStoreSnapshot(storeId = offer.storeId)
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)
        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(storeSnapshotDBPort.findById(offer.storeId)).thenReturn(snapshot)
        `when`(clock.instant()).thenReturn(now)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    offerId = offer.id,
                    reservationId = reservationId,
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("FoodBags can only be reserved from an ACTIVE Offer")
        assertThat(offer.availableQuantity).isEqualTo(5)
        assertThat(offer.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).getUserId()
        verify(reservationDBPort).findById(reservationId)
        verify(offerDBPort).findById(offer.id)
        verify(storeSnapshotDBPort).findById(offer.storeId)
        verify(clock).instant()
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            storeSnapshotDBPort,
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
    fun whenManagementRoleReservesFoodBags_throwsAccessDeniedException(role: ApplicationRole) {
        // Arrange
        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER))
            .thenReturn(role == ApplicationRole.CUSTOMER)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN))
            .thenReturn(role == ApplicationRole.ADMIN)

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                useCase.execute(
                    offerId = OfferId(UUID.randomUUID()),
                    reservationId = ReservationId(UUID.randomUUID()),
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot reserve FoodBags")

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).getUserId()
        verifyNoInteractions(
            offerDBPort,
            reservationDBPort,
            storeSnapshotDBPort,
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
                useCase.execute(
                    offerId = OfferId(UUID.randomUUID()),
                    reservationId = ReservationId(UUID.randomUUID()),
                    quantity = 2,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot reserve FoodBags")

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).getUserId()
        verifyNoInteractions(
            offerDBPort,
            reservationDBPort,
            storeSnapshotDBPort,
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
        status: OfferStatus = OfferStatus.ACTIVE,
        totalQuantity: Int = 5,
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
            status = status,
            totalQuantity = totalQuantity,
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

    private fun createStoreSnapshot(
        storeId: StoreId = StoreId(UUID.randomUUID()),
        partnerId: PartnerId = PartnerId(UUID.randomUUID()),
        partnerStatus: PartnerStatus = PartnerStatus.ACTIVE,
        storeStatus: StoreStatus = StoreStatus.ACTIVE,
    ): StoreSnapshot =
        StoreSnapshot(
            storeId = storeId,
            partnerId = partnerId,
            partnerStatus = partnerStatus,
            storeStatus = storeStatus,
            name = "Test store",
            address = "Test address",
            timeZone = ZoneId.of("Europe/Moscow"),
            storeVersion = 1,
            partnerVersion = 1,
        )

    private fun anyReservation(): OfferReservation =
        any(OfferReservation::class.java) ?: createReservation()

    companion object {
        private const val CURRENT_USER_ID = "33333333-3333-3333-3333-333333333333"
        private const val OTHER_USER_ID = "88888888-8888-8888-8888-888888888888"
    }
}
