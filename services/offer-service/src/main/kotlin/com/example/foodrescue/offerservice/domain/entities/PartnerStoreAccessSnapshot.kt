package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.enum.PartnerStatus
import com.example.foodrescue.offerservice.domain.enum.StoreStatus

data class PartnerStoreAccessSnapshot(
    val partnerStatus: PartnerStatus,
    val storeStatus: StoreStatus,
    val userIsStoreManager: Boolean,
    val userIsStoreStaff: Boolean,
)
