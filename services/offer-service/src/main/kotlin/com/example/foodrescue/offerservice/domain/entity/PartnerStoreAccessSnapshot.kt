package com.example.foodrescue.offerservice.domain.entity

import com.example.foodrescue.offerservice.domain.enum.PartnerStatus
import com.example.foodrescue.offerservice.domain.enum.StoreStatus

data class PartnerStoreAccessSnapshot(
    val partnerStatus: PartnerStatus,
    val storeStatus: StoreStatus,
    val storeBelongsToPartner: Boolean,
    val userIsManager: Boolean,
    val userIsStaff: Boolean,
)
