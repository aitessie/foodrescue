package com.example.foodrescue.partnerservice.adapter.`in`.dtos

import jakarta.validation.constraints.NotNull
import java.time.DayOfWeek
import java.time.LocalTime

class WorkingHoursDto(
    @field:NotNull var dayOfWeek: DayOfWeek,
    @field:NotNull var opensAt: LocalTime,
    @field:NotNull var closesAt: LocalTime,
)
