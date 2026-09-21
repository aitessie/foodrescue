package com.example.foodrescue.orderservice.adapter.out.http.dtos

import com.example.foodrescue.orderservice.domain.enum.PartnerStatus
import com.example.foodrescue.orderservice.domain.enum.StoreStatus

data class PartnerStoreAccessResponseDto(
    val partnerStatus: PartnerStatus,
    val storeStatus: StoreStatus,
    val userIsStoreManager: Boolean,
    val userIsStoreStaff: Boolean,
)
