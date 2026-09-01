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
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReleaseFoodBagReservationUseCase(
    private val offerDBPort: OfferDBPort,
    private val reservationDBPort: OfferReservationDBPort,
    private val currentUserPort: CurrentUserPort,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    @Transactional
    fun execute(reservationId: ReservationId): OfferReservation {
        validateRole()

        val reservation =
            reservationDBPort.findById(reservationId)
                ?: throw OfferReservationNotFoundException(reservationId)

        validateOwnership(reservation)

        if (reservation.status == ReservationStatus.RELEASED) {
            return reservation
        }

        val offer =
            offerDBPort.findById(reservation.offerId)
                ?: throw OfferNotFoundException(reservation.offerId)
        val now = clock.instant()

        releaseQuantity(
            offer = offer,
            reservation = reservation,
            now = now,
        )
        reservation.release(now)

        val savedOffer = offerDBPort.save(offer)
        val savedReservation = reservationDBPort.save(reservation)

        eventPublisherPort.publish(
            eventFactory.offerReservationReleased(
                offer = savedOffer,
                reservation = savedReservation,
                occurredAt = now,
            )
        )

        return savedReservation
    }

    private fun validateRole() {
        val hasAllowedRole =
            currentUserPort.hasRole(ApplicationRole.CUSTOMER) ||
                currentUserPort.hasRole(ApplicationRole.ADMIN)

        if (!hasAllowedRole) {
            throw AccessDeniedException("Current user cannot release OfferReservation")
        }
    }

    private fun validateOwnership(reservation: OfferReservation) {
        if (currentUserPort.hasRole(ApplicationRole.ADMIN)) {
            return
        }

        val currentUserId = currentUserPort.getUserId()

        if (reservation.customerId != currentUserId) {
            throw OfferReservationNotFoundException(reservation.id)
        }
    }

    private fun releaseQuantity(
        offer: Offer,
        reservation: OfferReservation,
        now: java.time.Instant,
    ) {
        try {
            offer.release(
                quantity = reservation.quantity,
                updatedAt = now,
            )
        } catch (exception: IllegalStateException) {
            throw InvalidStateException(exception.message ?: "Reserved FoodBags cannot be released")
        }
    }
}
