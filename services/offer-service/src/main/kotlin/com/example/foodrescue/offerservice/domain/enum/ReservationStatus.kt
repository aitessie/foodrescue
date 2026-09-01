package com.example.foodrescue.offerservice.domain.`enum`

enum class ReservationStatus(
    val code: String,
    val description: String,
) {
    RESERVED(
        "RESERVED",
        "Зарезервировано",
    ),
    RELEASED(
        "RELEASED",
        "Освобождено",
    ),
}
