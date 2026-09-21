package com.example.foodrescue.orderservice.adapter.out.http.dtos

import java.util.UUID

data class PartnerStoreAccessRequestDto(
    val storeId: UUID,
    val userId: String,
)
