package com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos

import java.util.UUID

data class StoreEventPayloadV1(
    val storeId: UUID,
    val partnerId: UUID,
    val partnerStatus: String,
    val storeStatus: String,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val timeZone: String,
    val rating: Double?,
)
