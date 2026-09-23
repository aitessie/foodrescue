package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.StoreAccessPolicy
import com.example.foodrescue.partnerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.partnerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.partnerservice.application.exceptions.PartnerNotFoundException
import com.example.foodrescue.partnerservice.application.exceptions.StoreNotFoundException
import com.example.foodrescue.partnerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateOrUpdateStoreUseCase(
    private val storeDBPort: StoreDBPort,
    private val partnerDBPort: PartnerDBPort,
    private val storeAccessPolicy: StoreAccessPolicy,
    private val eventFactory: ApplicationEventFactory,
    private val eventPublisherPort: DomainEventPublisherPort,
    private val clock: Clock,
) {

    @Transactional
    fun createOrUpdateStore(source: Store): Store {
        val existingStore = storeDBPort.findById(source.id)

        return if (existingStore == null) {
            createStore(source)
        } else {
            updateStore(
                existingStore = existingStore,
                source = source,
            )
        }
    }

    private fun createStore(source: Store): Store {
        val partner = getPartner(source.partnerId)

        storeAccessPolicy.checkAccess(
            action = AccessAction.CREATE_OR_UPDATE,
            resource = source,
        )

        val saved = storeDBPort.save(source)
        val now = clock.instant()

        eventPublisherPort.publish(
            eventFactory.storeCreated(
                store = saved,
                partner = partner,
                occurredAt = now,
            )
        )

        return saved
    }

    private fun updateStore(
        existingStore: Store,
        source: Store,
    ): Store {
        if (existingStore.partnerId != source.partnerId) {
            throw StoreNotFoundException(source.id)
        }

        storeAccessPolicy.checkAccess(
            action = AccessAction.CREATE_OR_UPDATE,
            resource = existingStore,
        )

        checkVersion(
            source = source,
            existingStore = existingStore,
        )

        val now = clock.instant()

        existingStore.updateFrom(
            source = source,
            updatedAt = now,
        )

        val saved = storeDBPort.save(existingStore)
        val partner = getPartner(saved.partnerId)

        val event =
            if (saved.status == StoreStatus.SUSPENDED) {
                eventFactory.storeSuspended(
                    store = saved,
                    partner = partner,
                    occurredAt = now,
                )
            } else {
                eventFactory.storeUpdated(
                    store = saved,
                    partner = partner,
                    occurredAt = now,
                )
            }

        eventPublisherPort.publish(event)

        return saved
    }

    private fun getPartner(partnerId: PartnerId): Partner =
        partnerDBPort.findById(partnerId) ?: throw PartnerNotFoundException(partnerId)

    private fun checkVersion(
        source: Store,
        existingStore: Store,
    ) {
        if (source.version != existingStore.version) {
            throw EntityVersionConflictException(
                entityType = Store::class.simpleName!!,
                entityId = existingStore.id.value.toString(),
                expectedVersion = source.version,
                actualVersion = existingStore.version,
            )
        }
    }
}
