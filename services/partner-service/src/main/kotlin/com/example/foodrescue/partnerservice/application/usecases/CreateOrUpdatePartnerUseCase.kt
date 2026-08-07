package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.PartnerAccessPolicy
import com.example.foodrescue.partnerservice.application.exception.EntityVersionConflictException
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.domain.entity.Partner
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class CreateOrUpdatePartnerUseCase(
    private val partnerDBPort: PartnerDBPort,
    private val partnerAccessPolicy: PartnerAccessPolicy,
    private val clock: Clock,
) {

    @Transactional
    fun createOrUpdatePartner(source: Partner): Partner {
        val existingPartner = partnerDBPort.findById(source.id)

        return if (existingPartner == null) {
            createPartner(source)
        } else {
            updatePartner(
                existingPartner = existingPartner,
                source = source,
            )
        }
    }

    private fun createPartner(source: Partner): Partner {
        partnerAccessPolicy.checkAccess(
            action = AccessAction.CREATE_OR_UPDATE,
            resource = source,
        )

        return partnerDBPort.save(source)
    }

    private fun updatePartner(
        existingPartner: Partner,
        source: Partner,
    ): Partner {
        partnerAccessPolicy.checkAccess(
            action = AccessAction.CREATE_OR_UPDATE,
            resource = existingPartner,
        )

        require(existingPartner.managerId == source.managerId) {
            "Partner managerId cannot be changed"
        }

        checkVersion(
            source = source,
            existingPartner = existingPartner,
        )

        existingPartner.updateFrom(
            source = source,
            updatedAt = Instant.now(clock),
        )

        return partnerDBPort.save(existingPartner)
    }

    private fun checkVersion(
        source: Partner,
        existingPartner: Partner
    ) {
        if (source.version != existingPartner.version) {
            throw EntityVersionConflictException(
                entityType = Partner::class.simpleName!!,
                entityId = existingPartner.id.value.toString(),
                expectedVersion = source.version,
                actualVersion = existingPartner.version,
            )
        }
    }

}
