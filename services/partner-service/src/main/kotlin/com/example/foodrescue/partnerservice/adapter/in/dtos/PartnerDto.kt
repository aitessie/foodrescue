package com.example.foodrescue.partnerservice.adapter.`in`.dtos

import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

class PartnerDto(
    @field:NotBlank @field:Size(max = 255) val managerId: String,
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotNull val status: PartnerStatus,
    @field:PositiveOrZero val version: Long = 0,
)
