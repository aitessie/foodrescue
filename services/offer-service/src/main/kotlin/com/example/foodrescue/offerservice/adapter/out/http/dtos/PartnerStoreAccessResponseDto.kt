package com.example.foodrescue.offerservice.adapter.out.http.dtos

import com.example.foodrescue.offerservice.domain.enum.PartnerStatus
import com.example.foodrescue.offerservice.domain.enum.StoreStatus

data class PartnerStoreAccessResponseDto(
    val partnerStatus: PartnerStatus,
    val storeStatus: StoreStatus,
    val userIsStoreManager: Boolean,
    val userIsStoreStaff: Boolean,
)
