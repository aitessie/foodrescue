package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.OfferReservationNotFoundException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProcessOrderReservationCommandUseCase(
    private val reserveFoodBagsUseCase: ReserveFoodBagsUseCase,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(
        reservationId: ReservationId,
        offerId: OfferId,
        customerId: String,
        quantity: Int,
    ) {
        logger.info(
            "Trying to process order reservation command: reservationId={}, offerId={}, quantity={}",
            reservationId.value,
            offerId.value,
            quantity,
        )

        try {
            val reservation =
                reserveFoodBagsUseCase.executeForCustomer(
                    offerId = offerId,
                    reservationId = reservationId,
                    customerId = customerId,
                    quantity = quantity,
                )

            logger.info(
                "Order reservation command processed successfully: reservationId={}, status={}",
                reservation.id.value,
                reservation.status,
            )
        } catch (exception: OfferNotFoundException) {
            reject(
                reservationId = reservationId,
                offerId = offerId,
                customerId = customerId,
                quantity = quantity,
                reason = exception.message ?: "Offer is unavailable for reservation",
            )
        } catch (exception: OfferReservationNotFoundException) {
            reject(
                reservationId = reservationId,
                offerId = offerId,
                customerId = customerId,
                quantity = quantity,
                reason = exception.message ?: "Reservation conflicts with an existing reservation",
            )
        } catch (exception: InvalidStateException) {
            reject(
                reservationId = reservationId,
                offerId = offerId,
                customerId = customerId,
                quantity = quantity,
                reason = exception.message ?: "Offer cannot be reserved",
            )
        }
    }

    private fun reject(
        reservationId: ReservationId,
        offerId: OfferId,
        customerId: String,
        quantity: Int,
        reason: String,
    ) {
        val occurredAt = clock.instant()

        eventPublisherPort.publish(
            eventFactory.offerReservationRejected(
                reservationId = reservationId,
                offerId = offerId,
                customerId = customerId,
                quantity = quantity,
                reason = reason,
                occurredAt = occurredAt,
            )
        )

        logger.info(
            "Order reservation command rejected: reservationId={}, offerId={}, reason={}",
            reservationId.value,
            offerId.value,
            reason,
        )
    }
}
