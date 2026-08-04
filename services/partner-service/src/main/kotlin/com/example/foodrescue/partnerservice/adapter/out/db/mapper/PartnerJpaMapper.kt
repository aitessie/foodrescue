package com.example.foodrescue.partnerservice.adapter.out.db.mapper

import com.example.foodrescue.partnerservice.adapter.out.db.entity.PartnerJpaEntity
import com.example.foodrescue.partnerservice.domain.entity.Partner
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import org.springframework.stereotype.Component

@Component
class PartnerJpaMapper {

    fun toEntity(partner: Partner): PartnerJpaEntity {
        return PartnerJpaEntity(
            id = partner.id.value,
            managerId = partner.managerId,
            name = partner.name,
            status = partner.status,
            createdAt = partner.createdAt,
            updatedAt = partner.updatedAt,
        )
    }

    fun toDomain(entity: PartnerJpaEntity): Partner {
        return Partner(
            id = PartnerId(entity.id),
            managerId = entity.managerId,
            name = entity.name,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
