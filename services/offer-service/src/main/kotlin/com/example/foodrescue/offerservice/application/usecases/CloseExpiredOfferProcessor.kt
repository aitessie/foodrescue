package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.domain.entities.OfferId
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class CloseExpiredOfferProcessor(
    private val offerDBPort: OfferDBPort,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun closeIfExpired(
        offerId: OfferId,
        now: Instant,
    ): Boolean {
        val offer = offerDBPort.findById(offerId) ?: return false

        val changed =
            offer.closeWhenExpired(
                now = now,
                updatedAt = now,
            )

        if (!changed) {
            return false
        }

        val savedOffer = offerDBPort.save(offer)

        eventPublisherPort.publish(
            eventFactory.offerClosed(
                offer = savedOffer,
                occurredAt = now,
            )
        )

        return true
    }
}
