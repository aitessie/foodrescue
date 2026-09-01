package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus

data class PartnerStatusSnapshotUpdate(
    val partnerId: PartnerId,
    val partnerStatus: PartnerStatus,
    val partnerVersion: Long,
)
