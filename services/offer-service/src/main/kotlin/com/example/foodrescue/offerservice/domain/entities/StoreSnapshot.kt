package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus
import com.example.foodrescue.offerservice.domain.`enum`.StoreStatus
import java.time.ZoneId

data class StoreSnapshot(
    val storeId: StoreId,
    val partnerId: PartnerId,
    val partnerStatus: PartnerStatus,
    val storeStatus: StoreStatus,
    val name: String,
    val address: String,
    val timeZone: ZoneId,
    val storeVersion: Long,
    val partnerVersion: Long,
)
