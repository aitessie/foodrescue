package com.example.foodrescue.offerservice.domain.entity

import com.example.foodrescue.offerservice.domain.enum.PartnerStatus
import com.example.foodrescue.offerservice.domain.enum.StoreStatus
import java.time.Instant
import java.time.ZoneId

data class StoreReadModel(
    val storeId: StoreId,
    val partnerId: PartnerId,
    val partnerStatus: PartnerStatus,
    val storeStatus: StoreStatus,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val timeZone: ZoneId,
    val rating: Double?,
    val storeVersion: Long,
    val partnerVersion: Long,
    val updatedAt: Instant,
)
