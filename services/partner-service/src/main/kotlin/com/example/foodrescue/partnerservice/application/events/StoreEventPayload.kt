package com.example.foodrescue.partnerservice.application.events

import java.util.UUID

data class StoreEventPayload(
    val storeId: UUID,
    val partnerId: UUID,
    val partnerStatus: String,
    val storeStatus: String,
    val name: String,
    val address: String,
) : ApplicationEventPayload
