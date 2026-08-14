package com.example.foodrescue.partnerservice.adapter.out.db

import com.example.foodrescue.partnerservice.adapter.out.db.mapper.PartnerJpaMapper
import com.example.foodrescue.partnerservice.adapter.out.db.persistence.PartnerJpaRepository
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import org.springframework.stereotype.Repository

@Repository
class PartnerRepository(
    private val jpaRepository: PartnerJpaRepository,
    private val mapper: PartnerJpaMapper,
) : PartnerDBPort {
    override fun save(partner: Partner): Partner {
        val entity = mapper.toEntity(partner)
        val savedEntity = jpaRepository.saveAndFlush(entity)

        return mapper.toDomain(savedEntity)
    }

    override fun findById(partnerId: PartnerId): Partner? =
        jpaRepository.findById(partnerId.value).map(mapper::toDomain).orElse(null)

    override fun findAllByManagerId(managerId: String): List<Partner> =
        jpaRepository.findAllByManagerId(managerId).map(mapper::toDomain)
}
