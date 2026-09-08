package com.example.foodrescue.offerservice.adapter.out.http.mappers

import com.example.foodrescue.offerservice.adapter.out.http.dtos.PartnerStoreAccessRequestDto
import com.example.foodrescue.offerservice.adapter.out.http.dtos.PartnerStoreAccessResponseDto
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PartnerStoreAccessSnapshot
import com.example.foodrescue.offerservice.domain.entities.StoreId
import org.springframework.stereotype.Component

@Component
class PartnerHttpMapper {
    fun toRequest(
        partnerId: PartnerId,
        storeId: StoreId,
        userId: String,
    ): PartnerStoreAccessRequestDto =
        PartnerStoreAccessRequestDto(
            partnerId = partnerId.value,
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
