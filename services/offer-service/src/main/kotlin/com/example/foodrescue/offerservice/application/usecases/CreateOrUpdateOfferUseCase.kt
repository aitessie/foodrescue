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
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateOrUpdateOfferUseCase(
    private val offerDBPort: OfferDBPort,
    private val foodBagDBPort: FoodBagDBPort,
    private val offerAccessPolicy: OfferAccessPolicy,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        partnerId: PartnerId,
        storeId: StoreId,
        offerId: OfferId,
        foodBagId: FoodBagId,
        totalQuantity: Int,
        pickupWindow: PickupWindow,
        requestedVersion: Long,
    ): Offer {
        val existing = offerDBPort.findById(offerId)

        validateOfferOwnership(
            existing = existing,
            storeId = storeId,
            offerId = offerId,
        )

        offerAccessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = storeId,
        )

        val foodBag =
            findActiveFoodBag(
                foodBagId = foodBagId,
                storeId = storeId,
            )

        return if (existing == null) {
            create(
                offerId = offerId,
                storeId = storeId,
                foodBag = foodBag,
                totalQuantity = totalQuantity,
                pickupWindow = pickupWindow,
                requestedVersion = requestedVersion,
            )
        } else {
            update(
                existing = existing,
                foodBagId = foodBagId,
                totalQuantity = totalQuantity,
                pickupWindow = pickupWindow,
                requestedVersion = requestedVersion,
            )
        }
    }

    private fun create(
        offerId: OfferId,
        storeId: StoreId,
        foodBag: FoodBag,
        totalQuantity: Int,
        pickupWindow: PickupWindow,
        requestedVersion: Long,
    ): Offer {
        validateCreateVersion(
            offerId = offerId,
            requestedVersion = requestedVersion,
        )

        val occurredAt = clock.instant()
        val offer =
            Offer(
                id = offerId,
                storeId = storeId,
                foodBagId = foodBag.id,
                category = foodBag.category,
                unitPrice = foodBag.unitPrice,
                allergens = foodBag.allergens,
                status = OfferStatus.SCHEDULED,
                totalQuantity = totalQuantity,
                availableQuantity = totalQuantity,
                pickupWindow = pickupWindow,
                createdAt = occurredAt,
                updatedAt = occurredAt,
                version = requestedVersion,
            )

        val savedOffer = offerDBPort.save(offer)

        eventPublisherPort.publish(
            eventFactory.offerCreated(
                offer = savedOffer,
                occurredAt = occurredAt,
            )
        )

        return savedOffer
    }

    private fun update(
        existing: Offer,
        foodBagId: FoodBagId,
        totalQuantity: Int,
        pickupWindow: PickupWindow,
        requestedVersion: Long,
    ): Offer {
        validateFoodBagIdentity(
            existing = existing,
            requestedFoodBagId = foodBagId,
        )
        validateVersion(
            existing = existing,
            requestedVersion = requestedVersion,
        )

        val occurredAt = clock.instant()
        val source =
            Offer(
                id = existing.id,
                storeId = existing.storeId,
                foodBagId = existing.foodBagId,
                category = existing.category,
                unitPrice = existing.unitPrice,
                allergens = existing.allergens,
                status = existing.status,
                totalQuantity = totalQuantity,
                availableQuantity = totalQuantity,
                pickupWindow = pickupWindow,
                createdAt = existing.createdAt,
                updatedAt = existing.updatedAt,
                version = requestedVersion,
            )

        existing.updateFrom(
            source = source,
            updatedAt = occurredAt,
        )

        val savedOffer = offerDBPort.save(existing)

        eventPublisherPort.publish(
            eventFactory.offerUpdated(
                offer = savedOffer,
                occurredAt = occurredAt,
            )
        )

        return savedOffer
    }

    private fun findActiveFoodBag(
        foodBagId: FoodBagId,
        storeId: StoreId,
    ): FoodBag {
        val foodBag = foodBagDBPort.findById(foodBagId) ?: throw FoodBagNotFoundException(foodBagId)

        if (foodBag.storeId != storeId) {
            throw FoodBagNotFoundException(foodBagId)
        }

        if (foodBag.status != FoodBagStatus.ACTIVE) {
            throw InvalidStateException("FoodBag must be ACTIVE to create or update an Offer")
        }

        return foodBag
    }

    private fun validateOfferOwnership(
        existing: Offer?,
        storeId: StoreId,
        offerId: OfferId,
    ) {
        if (existing != null && existing.storeId != storeId) {
            throw OfferNotFoundException(offerId)
        }
    }

    private fun validateCreateVersion(
        offerId: OfferId,
        requestedVersion: Long,
    ) {
        if (requestedVersion != 0L) {
            throw EntityVersionConflictException(
                entityName = "Offer",
                entityId = offerId.value,
                requestedVersion = requestedVersion,
                currentVersion = 0L,
            )
        }
    }

    private fun validateFoodBagIdentity(
        existing: Offer,
        requestedFoodBagId: FoodBagId,
    ) {
        if (existing.foodBagId != requestedFoodBagId) {
            throw OfferNotFoundException(existing.id)
        }
    }

    private fun validateVersion(
        existing: Offer,
        requestedVersion: Long,
    ) {
        if (requestedVersion != existing.version) {
            throw EntityVersionConflictException(
                entityName = "Offer",
                entityId = existing.id.value,
                requestedVersion = requestedVersion,
                currentVersion = existing.version,
            )
        }
    }
}
