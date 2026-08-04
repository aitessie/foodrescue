package com.example.foodrescue.partnerservice.adapter.`in`.dtos

import com.example.foodrescue.partnerservice.domain.entity.Address
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import com.example.foodrescue.partnerservice.domain.entity.StoreId
import com.example.foodrescue.partnerservice.domain.entity.WorkingHours
import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
import kotlin.time.Instant
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

class StoreDto(

    @field:NotNull
    val id: StoreId,

    @field:NotNull
    val partnerId: PartnerId,

    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    @field:NotNull
    val status: StoreStatus,

    @field:NotEmpty
    @field:Valid
    val workingHours: List<WorkingHours>,

    @field:NotNull
    @field:Valid
    val address: Address,

    @field:NotNull
    val createdAt: Instant,

    @field:NotNull
    val updatedAt: Instant,
) {
}
