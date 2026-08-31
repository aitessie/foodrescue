package com.example.foodrescue.offerservice.adapter.out.http.dtos

import java.util.UUID

data class PartnerStoreAccessRequestDto(
    val partnerId: UUID,
    val storeId: UUID,
    val userId: String,
)
