package com.example.foodrescue.partnerservice.adapter.out.db

import com.example.foodrescue.partnerservice.adapter.out.db.mapper.PartnerJpaMapper
import com.example.foodrescue.partnerservice.adapter.out.db.persistence.PartnerJpaRepository
import com.example.foodrescue.partnerservice.application.exception.PartnerNotFoundException
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.domain.entity.Partner
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import org.springframework.stereotype.Repository

@Repository
class PartnerRepository(
    private val jpaRepository: PartnerJpaRepository,
    private val mapper: PartnerJpaMapper,
) : PartnerDBPort {
    override fun save(partner: Partner): Partner {
        val entity = mapper.toEntity(partner)
        val savedEntity = jpaRepository.save(entity)

        return mapper.toDomain(savedEntity)
    }

    override fun findById(partnerId: PartnerId): Partner {
        return jpaRepository.findById(partnerId.value)
            .map(mapper::toDomain)
            .orElseThrow {
                PartnerNotFoundException(partnerId)
            }
    }

    override fun findAllByManagerId(managerId: String): List<Partner> {
        return jpaRepository.findAllByManagerId(managerId)
            .map(mapper::toDomain)
    }
}
