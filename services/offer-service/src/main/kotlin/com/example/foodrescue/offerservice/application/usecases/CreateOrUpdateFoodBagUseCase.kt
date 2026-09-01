package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.FoodBagAccessPolicy
import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.offerservice.application.exceptions.FoodBagNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.FoodBagDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateOrUpdateFoodBagUseCase(
    private val foodBagDBPort: FoodBagDBPort,
    private val foodBagAccessPolicy: FoodBagAccessPolicy,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        partnerId: PartnerId,
        source: FoodBag,
    ): FoodBag {
        val existing = foodBagDBPort.findById(source.id)

        if (existing != null && existing.storeId != source.storeId) {
            throw FoodBagNotFoundException(source.id)
        }

        foodBagAccessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = source.storeId,
        )

        return if (existing == null) {
            create(source)
        } else {
            update(
                existing = existing,
                source = source,
            )
        }
    }

    private fun create(source: FoodBag): FoodBag {
        validateCreateVersion(source)
        validateCreateStatus(source)

        val occurredAt = clock.instant()
        val savedFoodBag = foodBagDBPort.save(source)

        eventPublisherPort.publish(
            eventFactory.foodBagCreated(
                foodBag = savedFoodBag,
                occurredAt = occurredAt,
            )
        )

        return savedFoodBag
    }

    private fun update(
        existing: FoodBag,
        source: FoodBag,
    ): FoodBag {
        validateVersion(
            existing = existing,
            requestedVersion = source.version,
        )

        val occurredAt = clock.instant()

        existing.updateFrom(
            source = source,
            updatedAt = occurredAt,
        )

        val savedFoodBag = foodBagDBPort.save(existing)

        eventPublisherPort.publish(
            eventFactory.foodBagUpdated(
                foodBag = savedFoodBag,
                occurredAt = occurredAt,
            )
        )

        return savedFoodBag
    }

    private fun validateCreateVersion(source: FoodBag) {
        if (source.version != 0L) {
            throw EntityVersionConflictException(
                entityName = "FoodBag",
                entityId = source.id.value,
                requestedVersion = source.version,
                currentVersion = 0L,
            )
        }
    }

    private fun validateCreateStatus(source: FoodBag) {
        if (source.status != FoodBagStatus.ACTIVE) {
            throw InvalidStateException("New FoodBag must have ACTIVE status")
        }
    }

    private fun validateVersion(
        existing: FoodBag,
        requestedVersion: Long,
    ) {
        if (requestedVersion != existing.version) {
            throw EntityVersionConflictException(
                entityName = "FoodBag",
                entityId = existing.id.value,
                requestedVersion = requestedVersion,
                currentVersion = existing.version,
            )
        }
    }
}
