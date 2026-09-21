package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.OfferReservationNotFoundException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ProcessOrderReservationCommandUseCaseTest {
    @Mock private lateinit var reserveFoodBagsUseCase: ReserveFoodBagsUseCase

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: ProcessOrderReservationCommandUseCase

    @Test
    fun whenReservationCommandSucceeds_doesNotPublishRejectionEvent() {
        // Arrange
        val reservation = createReservation()

        `when`(
                reserveFoodBagsUseCase.executeForCustomer(
                    offerId = reservation.offerId,
                    reservationId = reservation.id,
                    customerId = CUSTOMER_ID,
                    quantity = reservation.quantity,
                )
            )
            .thenReturn(reservation)

        // Act
        useCase.execute(
            reservationId = reservation.id,
            offerId = reservation.offerId,
            customerId = CUSTOMER_ID,
            quantity = reservation.quantity,
        )

        // Assert
        verify(reserveFoodBagsUseCase)
            .executeForCustomer(
                offerId = reservation.offerId,
                reservationId = reservation.id,
                customerId = CUSTOMER_ID,
                quantity = reservation.quantity,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(reserveFoodBagsUseCase)
    }

    @Test
    fun whenOfferIsNotFound_publishesReservationRejectedEvent() {
        // Arrange
        val reservationId = ReservationId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val quantity = 2
        val exception = OfferNotFoundException(offerId)
        val reason = exception.message!!
        val occurredAt = Instant.parse("2026-08-20T11:00:00Z")
        val event =
            ApplicationEventFactory()
                .offerReservationRejected(
                    reservationId = reservationId,
                    offerId = offerId,
                    customerId = CUSTOMER_ID,
                    quantity = quantity,
                    reason = reason,
                    occurredAt = occurredAt,
                )

        `when`(
                reserveFoodBagsUseCase.executeForCustomer(
                    offerId = offerId,
                    reservationId = reservationId,
                    customerId = CUSTOMER_ID,
                    quantity = quantity,
                )
            )
            .thenThrow(exception)
        `when`(clock.instant()).thenReturn(occurredAt)
        `when`(
                eventFactory.offerReservationRejected(
                    reservationId = reservationId,
                    offerId = offerId,
                    customerId = CUSTOMER_ID,
                    quantity = quantity,
                    reason = reason,
                    occurredAt = occurredAt,
                )
            )
            .thenReturn(event)

        // Act
        useCase.execute(
            reservationId = reservationId,
            offerId = offerId,
            customerId = CUSTOMER_ID,
            quantity = quantity,
        )

        // Assert
        verify(reserveFoodBagsUseCase)
            .executeForCustomer(
                offerId = offerId,
                reservationId = reservationId,
                customerId = CUSTOMER_ID,
                quantity = quantity,
            )
        verify(clock).instant()
        verify(eventFactory)
            .offerReservationRejected(
                reservationId = reservationId,
                offerId = offerId,
                customerId = CUSTOMER_ID,
                quantity = quantity,
                reason = reason,
                occurredAt = occurredAt,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            reserveFoodBagsUseCase,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenReservationConflictsWithExistingReservation_publishesReservationRejectedEvent() {
        // Arrange
        val reservationId = ReservationId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val quantity = 2
        val exception = OfferReservationNotFoundException(reservationId)
        val reason = exception.message!!
        val occurredAt = Instant.parse("2026-08-20T11:00:00Z")
        val event =
            ApplicationEventFactory()
                .offerReservationRejected(
                    reservationId = reservationId,
                    offerId = offerId,
                    customerId = CUSTOMER_ID,
                    quantity = quantity,
                    reason = reason,
                    occurredAt = occurredAt,
                )

        `when`(
                reserveFoodBagsUseCase.executeForCustomer(
                    offerId = offerId,
                    reservationId = reservationId,
                    customerId = CUSTOMER_ID,
                    quantity = quantity,
                )
            )
            .thenThrow(exception)
        `when`(clock.instant()).thenReturn(occurredAt)
        `when`(
                eventFactory.offerReservationRejected(
                    reservationId = reservationId,
                    offerId = offerId,
                    customerId = CUSTOMER_ID,
                    quantity = quantity,
                    reason = reason,
                    occurredAt = occurredAt,
                )
            )
            .thenReturn(event)

        // Act
        useCase.execute(
            reservationId = reservationId,
            offerId = offerId,
            customerId = CUSTOMER_ID,
            quantity = quantity,
        )

        // Assert
        verify(reserveFoodBagsUseCase)
            .executeForCustomer(
                offerId = offerId,
                reservationId = reservationId,
                customerId = CUSTOMER_ID,
                quantity = quantity,
            )
        verify(clock).instant()
        verify(eventFactory)
            .offerReservationRejected(
                reservationId = reservationId,
                offerId = offerId,
                customerId = CUSTOMER_ID,
                quantity = quantity,
                reason = reason,
                occurredAt = occurredAt,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            reserveFoodBagsUseCase,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenOfferCannotBeReserved_publishesReservationRejectedEvent() {
        // Arrange
        val reservationId = ReservationId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val quantity = 2
        val exception = InvalidStateException("Requested quantity exceeds available Offer quantity")
        val reason = exception.message!!
        val occurredAt = Instant.parse("2026-08-20T11:00:00Z")
        val event =
            ApplicationEventFactory()
                .offerReservationRejected(
                    reservationId = reservationId,
                    offerId = offerId,
                    customerId = CUSTOMER_ID,
                    quantity = quantity,
                    reason = reason,
                    occurredAt = occurredAt,
                )

        `when`(
                reserveFoodBagsUseCase.executeForCustomer(
                    offerId = offerId,
                    reservationId = reservationId,
                    customerId = CUSTOMER_ID,
                    quantity = quantity,
                )
            )
            .thenThrow(exception)
        `when`(clock.instant()).thenReturn(occurredAt)
        `when`(
                eventFactory.offerReservationRejected(
                    reservationId = reservationId,
                    offerId = offerId,
                    customerId = CUSTOMER_ID,
                    quantity = quantity,
                    reason = reason,
                    occurredAt = occurredAt,
                )
            )
            .thenReturn(event)

        // Act
        useCase.execute(
            reservationId = reservationId,
            offerId = offerId,
            customerId = CUSTOMER_ID,
            quantity = quantity,
        )

        // Assert
        verify(reserveFoodBagsUseCase)
            .executeForCustomer(
                offerId = offerId,
                reservationId = reservationId,
                customerId = CUSTOMER_ID,
                quantity = quantity,
            )
        verify(clock).instant()
        verify(eventFactory)
            .offerReservationRejected(
                reservationId = reservationId,
                offerId = offerId,
                customerId = CUSTOMER_ID,
                quantity = quantity,
                reason = reason,
                occurredAt = occurredAt,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            reserveFoodBagsUseCase,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

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
