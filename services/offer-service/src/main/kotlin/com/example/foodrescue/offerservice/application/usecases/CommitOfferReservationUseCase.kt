package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.OfferReservationNotFoundException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.application.ports.OfferReservationDBPort
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommitOfferReservationUseCase(
    private val offerDBPort: OfferDBPort,
    private val reservationDBPort: OfferReservationDBPort,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(
        reservationId: ReservationId,
        offerId: OfferId,
    ): OfferReservation {
        logger.info(
            "Trying to commit Offer reservation: reservationId={}, offerId={}",
            reservationId.value,
            offerId.value,
        )

        val reservation =
            reservationDBPort.findById(reservationId)
                ?: throw OfferReservationNotFoundException(reservationId)

        if (reservation.offerId != offerId) {
            throw InvalidStateException("Reservation belongs to another Offer")
        }

        if (reservation.status == ReservationStatus.COMMITTED) {
            logger.info(
                "Offer reservation already committed: reservationId={}, offerId={}",
                reservation.id.value,
                reservation.offerId.value,
            )
            return reservation
        }

        val now = clock.instant()
        commitReservation(
            reservation = reservation,
            now = now,
        )

        val offer = offerDBPort.findById(offerId) ?: throw OfferNotFoundException(offerId)
        val savedReservation = reservationDBPort.save(reservation)

        eventPublisherPort.publish(
            eventFactory.offerReservationCommitted(
                offer = offer,
                reservation = savedReservation,
                occurredAt = now,
            )
        )

        logger.info(
            "Offer reservation committed successfully: reservationId={}, status={}",
            savedReservation.id.value,
            savedReservation.status,
        )
        return savedReservation
    }

    private fun commitReservation(
        reservation: OfferReservation,
        now: java.time.Instant,
    ) {
        try {
            reservation.commit(now)
        } catch (exception: IllegalStateException) {
            throw InvalidStateException(
                exception.message ?: "Offer reservation cannot be committed"
            )
        }
    }
}
