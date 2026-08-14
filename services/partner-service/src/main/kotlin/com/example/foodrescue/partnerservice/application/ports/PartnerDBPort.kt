package com.example.foodrescue.partnerservice.application.ports

import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId

interface PartnerDBPort {
    fun save(partner: Partner): Partner

    fun findById(partnerId: PartnerId): Partner?

    fun findAllByManagerId(managerId: String): List<Partner>
}
