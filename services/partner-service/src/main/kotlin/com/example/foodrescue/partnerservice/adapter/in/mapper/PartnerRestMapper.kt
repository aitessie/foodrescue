package com.example.foodrescue.partnerservice.adapter.`in`.mapper

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.PartnerDto
import com.example.foodrescue.partnerservice.domain.entity.Partner
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import org.springframework.stereotype.Component

@Component
class PartnerRestMapper {

    fun toDto(partner: Partner): PartnerDto {
        return PartnerDto(
            id = partner.id.value,
            managerId = partner.managerId,
            name = partner.name,
            status = partner.status,
            createdAt = partner.createdAt,
            updatedAt = partner.updatedAt,
        )
    }

    fun toDomain(dto: PartnerDto): Partner {
        return Partner(
            id = PartnerId(dto.id),
            managerId = dto.managerId,
            name = dto.name,
            status = dto.status,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
        )
    }
}
