package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.PartnerAccessPolicy
import com.example.foodrescue.partnerservice.application.exception.PartnerNotFoundException
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
        val existingPartner = try {
            partnerDBPort.findById(source.id)
        } catch (_: PartnerNotFoundException) {
            null
        }

        partnerAccessPolicy.checkAccess(
            action = AccessAction.CREATE_OR_UPDATE,
            resource = source,
        )

        return if (existingPartner == null) {
            partnerDBPort.save(source)
        } else {
            existingPartner.updateFrom(
                source,
                updatedAt = Instant.now(clock),
            )
            partnerDBPort.save(existingPartner)
        }
    }
}
