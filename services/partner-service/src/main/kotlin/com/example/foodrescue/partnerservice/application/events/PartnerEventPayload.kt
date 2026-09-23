package com.example.foodrescue.partnerservice.application.events

import java.util.UUID

data class PartnerEventPayload(
    val partnerId: UUID,
    val partnerStatus: String,
) : ApplicationEventPayload
