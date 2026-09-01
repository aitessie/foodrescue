package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.FoodBagAccessPolicy
import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.offerservice.application.exceptions.FoodBagNotFoundException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.FoodBagDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChangeFoodBagStatusUseCase(
    private val foodBagDBPort: FoodBagDBPort,
    private val accessPolicy: FoodBagAccessPolicy,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        partnerId: PartnerId,
        storeId: StoreId,
        foodBagId: FoodBagId,
        targetStatus: FoodBagStatus,
        expectedVersion: Long,
    ): FoodBag {
        val existing =
            foodBagDBPort.findById(foodBagId) ?: throw FoodBagNotFoundException(foodBagId)

        if (existing.storeId != storeId) {
            throw FoodBagNotFoundException(foodBagId)
        }

        accessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = storeId,
        )
        validateVersion(
            foodBag = existing,
            expectedVersion = expectedVersion,
        )

        val now = clock.instant()
        val changed =
            existing.changeStatus(
                targetStatus = targetStatus,
                updatedAt = now,
            )

        if (!changed) {
            return existing
        }

        val saved = foodBagDBPort.save(existing)

        eventPublisherPort.publish(
            eventFactory.foodBagStatusChanged(
                foodBag = saved,
                occurredAt = now,
            )
        )

        return saved
    }

    private fun validateVersion(
        foodBag: FoodBag,
        expectedVersion: Long,
    ) {
        val version = foodBag.version
        if (version != expectedVersion) {
            throw EntityVersionConflictException(
                entityName = "FoodBag",
                entityId = foodBag.id.value,
                requestedVersion = expectedVersion,
                currentVersion = version,
            )
        }
    }
}
