package com.example.foodrescue.offerservice.domain.entities

import java.time.Instant

data class PickupWindow(
    val start: Instant,
    val end: Instant,
) {
    init {
        require(end.isAfter(start)) {
            "Pickup window end must be strictly later than start"
        }
    }
}
