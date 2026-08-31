package com.example.foodrescue.offerservice.adapter.out.http.dtos

data class PartnerStoreAccessResponseDto(
    val partnerStatus: String?,
    val storeStatus: String?,
    val storeBelongsToPartner: Boolean?,
    val userIsManager: Boolean?,
    val userIsStaff: Boolean?,
)
