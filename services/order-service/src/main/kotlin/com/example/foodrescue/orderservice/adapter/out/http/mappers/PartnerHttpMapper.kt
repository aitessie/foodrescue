package com.example.foodrescue.orderservice.adapter.out.http.mappers

import com.example.foodrescue.orderservice.adapter.out.http.dtos.PartnerStoreAccessRequestDto
import com.example.foodrescue.orderservice.adapter.out.http.dtos.PartnerStoreAccessResponseDto
import com.example.foodrescue.orderservice.domain.entities.PartnerStoreAccessSnapshot
import com.example.foodrescue.orderservice.domain.entities.StoreId
import org.springframework.stereotype.Component

@Component
class PartnerHttpMapper {
    fun toRequest(
        storeId: StoreId,
        userId: String,
    ): PartnerStoreAccessRequestDto =
        PartnerStoreAccessRequestDto(
            storeId = storeId.value,
            userId = userId,
        )

    fun toSnapshot(response: PartnerStoreAccessResponseDto): PartnerStoreAccessSnapshot =
        PartnerStoreAccessSnapshot(
            partnerStatus = response.partnerStatus,
            storeStatus = response.storeStatus,
            userIsStoreManager = response.userIsStoreManager,
            userIsStoreStaff = response.userIsStoreStaff,
        )
}
