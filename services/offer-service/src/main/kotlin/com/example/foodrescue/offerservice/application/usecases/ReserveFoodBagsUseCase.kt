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
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus
import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
import com.example.foodrescue.offerservice.domain.`enum`.StoreStatus
import java.time.Clock
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReserveFoodBagsUseCase(
    private val offerDBPort: OfferDBPort,
    private val reservationDBPort: OfferReservationDBPort,
    private val storeSnapshotDBPort: StoreSnapshotDBPort,
    private val currentUserPort: CurrentUserPort,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        offerId: OfferId,
        reservationId: ReservationId,
        quantity: Int,
    ): OfferReservation {
        validateRole()

        val customerId = currentUserPort.getUserId()
        val existingReservation = reservationDBPort.findById(reservationId)

        if (existingReservation != null) {
            return validateIdempotentRequest(
                reservation = existingReservation,
                offerId = offerId,
                customerId = customerId,
                quantity = quantity,
            )
        }

        val offer = offerDBPort.findById(offerId) ?: throw OfferNotFoundException(offerId)

        validateStoreSnapshot(offer)

        val now = clock.instant()

        reserveQuantity(
            offer = offer,
            quantity = quantity,
            now = now,
        )

        val reservation =
            OfferReservation(
                id = reservationId,
                offerId = offer.id,
                customerId = customerId,
                quantity = quantity,
                status = ReservationStatus.RESERVED,
                createdAt = now,
                updatedAt = now,
                version = 0L,
            )

        val savedOffer = offerDBPort.save(offer)
        val savedReservation = reservationDBPort.save(reservation)

        eventPublisherPort.publish(
            eventFactory.offerReserved(
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
            throw AccessDeniedException("Current user cannot reserve FoodBags")
        }
    }

    private fun validateIdempotentRequest(
        reservation: OfferReservation,
        offerId: OfferId,
        customerId: String,
        quantity: Int,
    ): OfferReservation {
        if (reservation.customerId != customerId) {
            throw OfferReservationNotFoundException(reservation.id)
        }

        if (reservation.offerId != offerId) {
            throw InvalidStateException("Reservation already belongs to another Offer")
        }

        if (reservation.quantity != quantity) {
            throw InvalidStateException(
                "Reservation quantity does not match the existing reservation"
            )
        }

        if (reservation.status != ReservationStatus.RESERVED) {
            throw InvalidStateException("Released reservation cannot be reserved again")
        }

        return reservation
    }

    private fun validateStoreSnapshot(offer: Offer) {
        val snapshot =
            storeSnapshotDBPort.findById(offer.storeId) ?: throw OfferNotFoundException(offer.id)

        if (
            snapshot.partnerStatus != PartnerStatus.ACTIVE ||
                snapshot.storeStatus != StoreStatus.ACTIVE
        ) {
            throw OfferNotFoundException(offer.id)
        }
    }

    private fun reserveQuantity(
        offer: Offer,
        quantity: Int,
        now: Instant,
    ) {
        try {
            offer.reserve(
                quantity = quantity,
                now = now,
                updatedAt = now,
            )
        } catch (exception: IllegalArgumentException) {
            throw InvalidStateException(exception.message ?: "FoodBags cannot be reserved")
        } catch (exception: IllegalStateException) {
            throw InvalidStateException(exception.message ?: "FoodBags cannot be reserved")
        }
    }
}
