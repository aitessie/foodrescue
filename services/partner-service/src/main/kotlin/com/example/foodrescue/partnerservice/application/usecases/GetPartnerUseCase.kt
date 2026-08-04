package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.PartnerAccessPolicy
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.domain.entity.Partner
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import org.springframework.stereotype.Service

@Service
class GetPartnerUseCase(
    private val partnerDBPort: PartnerDBPort,
    private val partnerAccessPolicy: PartnerAccessPolicy,
) {

    fun getPartner(partnerId: PartnerId): Partner {
        val partner = partnerDBPort.findById(partnerId)

        partnerAccessPolicy.checkAccess(
            action = AccessAction.READ,
            resource = partner,
        )

        return partner
    }
}
