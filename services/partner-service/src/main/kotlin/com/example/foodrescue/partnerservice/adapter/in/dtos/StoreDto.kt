package com.example.foodrescue.partnerservice.adapter.`in`.dtos

import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
import java.time.Instant
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.util.UUID

class StoreDto(

    @field:NotNull
    val id: UUID,

    @field:NotNull
    val partnerId: UUID,

    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    @field:NotNull
    val status: StoreStatus,

    @field:NotEmpty
    @field:Valid
    val workingHours: List<WorkingHoursDto>,

    @field:NotNull
    @field:Valid
    val address: AddressDto,

    @field:NotNull
    val createdAt: Instant,

    @field:NotNull
    val updatedAt: Instant,

    @field:PositiveOrZero
    val version: Long = 0,
)
