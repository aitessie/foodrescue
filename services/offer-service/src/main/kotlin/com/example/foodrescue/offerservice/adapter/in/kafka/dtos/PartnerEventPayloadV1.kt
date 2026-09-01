package com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos

import java.util.UUID

data class PartnerEventPayloadV1(
    val partnerId: UUID,
    val partnerStatus: String,
)
