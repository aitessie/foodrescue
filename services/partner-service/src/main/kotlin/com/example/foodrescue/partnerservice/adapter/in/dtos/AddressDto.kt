package com.example.foodrescue.partnerservice.adapter.`in`.dtos

import jakarta.validation.constraints.NotBlank

class AddressDto(
    @field:NotBlank var city: String,
    @field:NotBlank var street: String,
    @field:NotBlank var building: String,
    @field:NotBlank var postalCode: String?,
)
