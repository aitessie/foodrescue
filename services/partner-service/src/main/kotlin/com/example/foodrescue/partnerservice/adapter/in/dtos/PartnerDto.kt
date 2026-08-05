package com.example.foodrescue.partnerservice.adapter.`in`.dtos

import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.util.UUID
import java.time.Instant

class PartnerDto(
    @field:NotNull
    val id: UUID,

    @field:NotBlank
    val managerId: String,

    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    @field:NotNull
    val status: PartnerStatus,

    @field:NotNull
    val createdAt: Instant,

    @field:NotNull
    val updatedAt: Instant,

    @field:PositiveOrZero
    val version: Long = 0,
)
