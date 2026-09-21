package com.example.foodrescue.orderservice.domain.entities

import com.example.foodrescue.orderservice.domain.enum.PartnerStatus
import com.example.foodrescue.orderservice.domain.enum.StoreStatus

data class PartnerStoreAccessSnapshot(
    val partnerStatus: PartnerStatus,
    val storeStatus: StoreStatus,
    val userIsStoreManager: Boolean,
    val userIsStoreStaff: Boolean,
)
