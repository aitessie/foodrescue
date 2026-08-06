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
        val existingStore = try {
            storeDBPort.findById(source.id)
        } catch (_: StoreNotFoundException) {
            null
        }

        storeAccessPolicy.checkAccess(
            action = AccessAction.CREATE_OR_UPDATE,
            resource = source,
        )

        return if (existingStore == null) {
            storeDBPort.save(source)
        } else {
            checkVersion(source, existingStore)
            existingStore.updateFrom(
                source,
                updatedAt = Instant.now(clock),
            )
            storeDBPort.save(existingStore)
        }
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
