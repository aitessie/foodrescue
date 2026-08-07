package com.example.foodrescue.partnerservice.adapter.`in`.dtos

import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

class StoreDto(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotNull val status: StoreStatus,
    @field:NotEmpty @field:Valid val workingHours: List<WorkingHoursDto>,
    @field:NotNull @field:Valid val address: AddressDto,
    @field:PositiveOrZero val version: Long = 0,
)
