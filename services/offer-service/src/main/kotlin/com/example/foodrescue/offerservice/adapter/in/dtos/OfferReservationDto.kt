package com.example.foodrescue.offerservice.adapter.`in`.dtos

import com.example.foodrescue.offerservice.domain.`enum`.ReservationStatus
import java.time.Instant

data class OfferReservationDto(
    val quantity: Int,
    val status: ReservationStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
)
