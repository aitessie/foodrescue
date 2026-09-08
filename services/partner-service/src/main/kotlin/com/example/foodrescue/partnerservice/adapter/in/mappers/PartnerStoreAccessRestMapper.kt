package com.example.foodrescue.partnerservice.adapter.`in`.mappers

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.PartnerStoreAccessResponseDto
import com.example.foodrescue.partnerservice.domain.entities.PartnerStoreAccessSnapshot
import org.springframework.stereotype.Component

@Component
class PartnerStoreAccessRestMapper {
    fun toDto(snapshot: PartnerStoreAccessSnapshot): PartnerStoreAccessResponseDto =
        PartnerStoreAccessResponseDto(
            partnerStatus = snapshot.partnerStatus,
            storeStatus = snapshot.storeStatus,
            userIsStoreManager = snapshot.userIsStoreManager,
            userIsStoreStaff = snapshot.userIsStoreStaff,
        )
}
