package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.OfferAccessPolicy
import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.offerservice.application.exceptions.FoodBagNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.FoodBagDBPort
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChangeOfferStatusUseCase(
    private val offerDBPort: OfferDBPort,
    private val foodBagDBPort: FoodBagDBPort,
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
        targetStatus: OfferStatus,
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

        if (offer.status == targetStatus) {
            return offer
        }

        val foodBagStatus =
            resolveFoodBagStatus(
                foodBagId = offer.foodBagId,
                storeId = storeId,
                targetStatus = targetStatus,
            )
        val now = clock.instant()

        try {
            offer.changeStatus(
                targetStatus = targetStatus,
                foodBagStatus = foodBagStatus,
                now = now,
                updatedAt = now,
            )
        } catch (exception: IllegalStateException) {
            throw InvalidStateException(exception.message ?: "Offer status transition is invalid")
        }

        val saved = offerDBPort.save(offer)

        eventPublisherPort.publish(
            eventFactory.offerStatusChanged(
                offer = saved,
                occurredAt = now,
            )
        )

        return saved
    }

    private fun resolveFoodBagStatus(
        foodBagId: FoodBagId,
        storeId: StoreId,
        targetStatus: OfferStatus,
    ): FoodBagStatus {
        if (targetStatus != OfferStatus.ACTIVE) {
            return FoodBagStatus.INACTIVE
        }

        val foodBag = foodBagDBPort.findById(foodBagId) ?: throw FoodBagNotFoundException(foodBagId)

        if (foodBag.storeId != storeId) {
            throw FoodBagNotFoundException(foodBagId)
        }

        return foodBag.status
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
