package com.example.foodrescue.partnerservice.application.ports

import com.example.foodrescue.partnerservice.domain.entity.Partner
import com.example.foodrescue.partnerservice.domain.entity.PartnerId

interface PartnerDBPort {
    fun save(partner: Partner): Partner
    fun findById(partnerId: PartnerId): Partner
    fun findAllByManagerId(managerId: String): List<Partner>
}
