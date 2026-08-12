package com.example.foodrescue.offerservice.domain.entity

import com.example.foodrescue.offerservice.domain.enum.PartnerStatus
import java.time.Instant

data class PartnerStatusProjectionUpdate(
    val partnerId: PartnerId,
    val partnerStatus: PartnerStatus,
    val partnerVersion: Long,
    val occurredAt: Instant,
)
