package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.OfferAccessPolicy
import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.ValidationException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChangeOfferQuantityUseCase(
    private val offerDBPort: OfferDBPort,
    private val accessPolicy: OfferAccessPolicy,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        partnerId: PartnerId,
        storeId: StoreId,
        offerId: OfferId,
        totalQuantity: Int,
        expectedVersion: Long,
    ): Offer {
        val offer = offerDBPort.findById(offerId) ?: throw OfferNotFoundException(offerId)

        if (offer.storeId != storeId) {
            throw OfferNotFoundException(offerId)
        }

        accessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = storeId,
        )
        validateVersion(
            offer = offer,
            expectedVersion = expectedVersion,
        )

        if (offer.totalQuantity == totalQuantity) {
            return offer
        }

        val now = clock.instant()

        try {
            offer.changeTotalQuantity(
                quantity = totalQuantity,
                updatedAt = now,
            )
        } catch (exception: IllegalArgumentException) {
            throw ValidationException(exception.message ?: "Offer totalQuantity is invalid")
        } catch (exception: IllegalStateException) {
            throw InvalidStateException(
                exception.message ?: "Offer totalQuantity cannot be changed"
            )
        }

        val saved = offerDBPort.save(offer)

        eventPublisherPort.publish(
            eventFactory.offerQuantityChanged(
                offer = saved,
                occurredAt = now,
            )
        )

        return saved
    }

    private fun validateVersion(
        offer: Offer,
        expectedVersion: Long,
    ) {
        if (offer.version != expectedVersion) {
            throw EntityVersionConflictException(
                entityName = "Offer",
                entityId = offer.id.value,
                requestedVersion = expectedVersion,
                currentVersion = offer.version,
            )
        }
    }
}
