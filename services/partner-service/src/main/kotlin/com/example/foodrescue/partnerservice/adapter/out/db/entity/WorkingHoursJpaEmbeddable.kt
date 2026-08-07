package com.example.foodrescue.partnerservice.adapter.out.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.DayOfWeek
import java.time.LocalTime

@Embeddable
class WorkingHoursJpaEmbeddable(
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    var dayOfWeek: DayOfWeek,
    @Column(name = "opens_at", nullable = false) var opensAt: LocalTime,
    @Column(name = "closes_at", nullable = false) var closesAt: LocalTime,
)
