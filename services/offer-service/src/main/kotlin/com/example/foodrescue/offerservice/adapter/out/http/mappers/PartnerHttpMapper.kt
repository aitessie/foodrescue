package com.example.foodrescue.offerservice.adapter.out.http.mappers

import com.example.foodrescue.offerservice.adapter.out.http.dtos.PartnerStoreAccessRequestDto
import com.example.foodrescue.offerservice.adapter.out.http.dtos.PartnerStoreAccessResponseDto
import com.example.foodrescue.offerservice.application.exceptions.PartnerServiceContractException
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PartnerStoreAccessSnapshot
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.enum.PartnerStatus
import com.example.foodrescue.offerservice.domain.enum.StoreStatus
import org.springframework.stereotype.Component

@Component
class PartnerHttpMapper {
    fun toRequest(
        partnerId: PartnerId,
        storeId: StoreId,
        userId: String,
    ): PartnerStoreAccessRequestDto {
        if (userId.isBlank()) {
            throw PartnerServiceContractException("Partner access-check userId must not be blank")
        }

        return PartnerStoreAccessRequestDto(
            partnerId = partnerId.value,
            storeId = storeId.value,
            userId = userId,
        )
    }

    fun toSnapshot(response: PartnerStoreAccessResponseDto): PartnerStoreAccessSnapshot =
        PartnerStoreAccessSnapshot(
            partnerStatus =
                parsePartnerStatus(
                    response.partnerStatus ?: throw invalidResponse("partnerStatus is required")
                ),
            storeStatus =
                parseStoreStatus(
                    response.storeStatus ?: throw invalidResponse("storeStatus is required")
                ),
            storeBelongsToPartner =
                response.storeBelongsToPartner
                    ?: throw invalidResponse("storeBelongsToPartner is required"),
            userIsManager =
                response.userIsManager ?: throw invalidResponse("userIsManager is required"),
            userIsStaff = response.userIsStaff ?: throw invalidResponse("userIsStaff is required"),
        )

    private fun parsePartnerStatus(value: String): PartnerStatus =
        PartnerStatus.entries.firstOrNull { status ->
            status.code == value
        } ?: throw invalidResponse("partnerStatus is unsupported")

    private fun parseStoreStatus(value: String): StoreStatus =
        StoreStatus.entries.firstOrNull { status ->
            status.code == value
        } ?: throw invalidResponse("storeStatus is unsupported")

    private fun invalidResponse(reason: String): PartnerServiceContractException =
        PartnerServiceContractException(
            "Partner Service returned an invalid access-check response: $reason"
        )
}
