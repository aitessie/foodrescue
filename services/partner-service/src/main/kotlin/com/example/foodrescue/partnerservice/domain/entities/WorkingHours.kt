package com.example.foodrescue.partnerservice.domain.entities

import java.time.DayOfWeek
import java.time.LocalTime

class WorkingHours(
    val dayOfWeek: DayOfWeek,
    val opensAt: LocalTime,
    val closesAt: LocalTime,
)
