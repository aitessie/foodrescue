package com.example.foodrescue.partnerservice.adapter.`in`.dtos

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class PartnerStoreAccessRequestDto(
    val partnerId: UUID,
    val storeId: UUID,
    @field:NotBlank val userId: String,
)
