package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.StoreAccessPolicy
import com.example.foodrescue.partnerservice.application.exception.EntityVersionConflictException
import com.example.foodrescue.partnerservice.application.exception.StoreNotFoundException
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.domain.entity.Store
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class CreateOrUpdateStoreUseCase(
    private val storeDBPort: StoreDBPort,
    private val storeAccessPolicy: StoreAccessPolicy,
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
        storeAccessPolicy.checkAccess(
            action = AccessAction.CREATE_OR_UPDATE,
            resource = source,
        )

        return storeDBPort.save(source)
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

        existingStore.updateFrom(
            source = source,
            updatedAt = Instant.now(clock),
        )

        return storeDBPort.save(existingStore)
    }

    private fun checkVersion(
        source: Store,
        existingStore: Store
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
