package com.example.foodrescue.offerservice.domain.enum

enum class DateFilter(
    val code: String,
    val description: String,
) {
    TODAY("TODAY", "Сегодня"),
    TOMORROW("TOMORROW", "Завтра"),
}
