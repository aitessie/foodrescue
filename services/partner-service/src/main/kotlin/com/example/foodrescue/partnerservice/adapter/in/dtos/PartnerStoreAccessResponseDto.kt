package com.example.foodrescue.partnerservice.adapter.`in`.dtos

import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import com.example.foodrescue.partnerservice.domain.enum.StoreStatus

data class PartnerStoreAccessResponseDto(
    val partnerStatus: PartnerStatus,
    val storeStatus: StoreStatus,
    val userIsStoreManager: Boolean,
    val userIsStoreStaff: Boolean,
)
