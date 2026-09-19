package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.OfferReservationNotFoundException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.application.ports.OfferReservationDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
import java.time.Clock
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
class CommitOfferReservationUseCaseTest {
    @Mock private lateinit var offerDBPort: OfferDBPort

    @Mock private lateinit var reservationDBPort: OfferReservationDBPort

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: CommitOfferReservationUseCase

    @Test
    fun whenReservedReservationIsCommitted_returnsSavedReservation() {
        // Arrange
        val reservation = createReservation()
        val offer = createOffer(id = reservation.offerId)
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedReservation =
            createReservation(
                id = reservation.id,
                offerId = reservation.offerId,
                customerId = reservation.customerId,
                quantity = reservation.quantity,
                status = ReservationStatus.COMMITTED,
                createdAt = reservation.createdAt,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .offerReservationCommitted(
                    offer = offer,
                    reservation = savedReservation,
                    occurredAt = now,
                )

        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerDBPort.findById(reservation.offerId)).thenReturn(offer)
        `when`(reservationDBPort.save(reservation)).thenReturn(savedReservation)
        `when`(
                eventFactory.offerReservationCommitted(
                    offer = offer,
                    reservation = savedReservation,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result =
            useCase.execute(
                reservationId = reservation.id,
                offerId = reservation.offerId,
            )

        // Assert
        assertThat(result).isSameAs(savedReservation)
        assertThat(reservation.status).isEqualTo(ReservationStatus.COMMITTED)
        assertThat(reservation.updatedAt).isEqualTo(now)

        verify(reservationDBPort).findById(reservation.id)
        verify(clock).instant()
        verify(offerDBPort).findById(reservation.offerId)
        verify(reservationDBPort).save(reservation)
        verify(eventFactory)
            .offerReservationCommitted(
                offer = offer,
                reservation = savedReservation,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenCommittedReservationIsCommittedAgain_returnsExistingReservation() {
        // Arrange
        val reservation = createReservation(status = ReservationStatus.COMMITTED)

        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)

        // Act
        val result =
            useCase.execute(
                reservationId = reservation.id,
                offerId = reservation.offerId,
            )

        // Assert
        assertThat(result).isSameAs(reservation)

        verify(reservationDBPort).findById(reservation.id)
        verifyNoInteractions(
            offerDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(reservationDBPort)
    }

    @Test
    fun whenReservationDoesNotExist_throwsOfferReservationNotFoundException() {
        // Arrange
        val reservationId = ReservationId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())

        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferReservationNotFoundException> {
                useCase.execute(
                    reservationId = reservationId,
                    offerId = offerId,
                )
            }

        // Assert
        assertThat(exception.message).contains(reservationId.value.toString())

        verify(reservationDBPort).findById(reservationId)
        verifyNoInteractions(
            offerDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(reservationDBPort)
    }

    @Test
    fun whenReservationBelongsToAnotherOffer_throwsInvalidStateException() {
        // Arrange
        val reservation = createReservation()
        val offerId = OfferId(UUID.randomUUID())

        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    reservationId = reservation.id,
                    offerId = offerId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Reservation belongs to another Offer")

        verify(reservationDBPort).findById(reservation.id)
        verifyNoInteractions(
            offerDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(reservationDBPort)
    }

    @Test
    fun whenReleasedReservationIsCommitted_throwsInvalidStateException() {
        // Arrange
        val reservation = createReservation(status = ReservationStatus.RELEASED)
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(clock.instant()).thenReturn(now)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    reservationId = reservation.id,
                    offerId = reservation.offerId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Only a reserved reservation can be committed")
        assertThat(reservation.status).isEqualTo(ReservationStatus.RELEASED)
        assertThat(reservation.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(reservationDBPort).findById(reservation.id)
        verify(clock).instant()
        verifyNoInteractions(
            offerDBPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            reservationDBPort,
            clock,
        )
    }

    @Test
    fun whenOfferDoesNotExist_throwsOfferNotFoundException() {
        // Arrange
        val reservation = createReservation()
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerDBPort.findById(reservation.offerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    reservationId = reservation.id,
                    offerId = reservation.offerId,
                )
            }

        // Assert
        assertThat(exception.message).contains(reservation.offerId.value.toString())

        verify(reservationDBPort).findById(reservation.id)
        verify(clock).instant()
        verify(offerDBPort).findById(reservation.offerId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            reservationDBPort,
            clock,
        )
    }

    private fun createOffer(
        id: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        foodBagId: FoodBagId = FoodBagId(UUID.randomUUID()),
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
    ): Offer =
        Offer(
            id = id,
            storeId = storeId,
            foodBagId = foodBagId,
            category = FoodBagCategory.entries.first(),
            unitPrice = 500,
            allergens = emptySet(),
            status = OfferStatus.ACTIVE,
            totalQuantity = 5,
            availableQuantity = 3,
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
        customerId: String = CUSTOMER_ID,
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
        private const val CUSTOMER_ID = "33333333-3333-3333-3333-333333333333"
    }
}
