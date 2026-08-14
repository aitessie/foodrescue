package com.example.foodrescue.partnerservice.adapter.`in`.mapper

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.PartnerDto
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import java.time.Clock
import java.time.Instant
import org.springframework.stereotype.Component

@Component
class PartnerRestMapper(private val clock: Clock) {

    fun toDomain(
        dto: PartnerDto,
        partnerId: PartnerId,
    ): Partner {
        val now = Instant.now(clock)

        return Partner(
            id = partnerId,
            managerId = dto.managerId,
            name = dto.name,
            status = dto.status,
            createdAt = now,
            updatedAt = now,
            version = dto.version,
        )
    }

    fun toDto(partner: Partner): PartnerDto {
        return PartnerDto(
            managerId = partner.managerId,
            name = partner.name,
            status = partner.status,
            version = partner.version,
        )
    }
}
