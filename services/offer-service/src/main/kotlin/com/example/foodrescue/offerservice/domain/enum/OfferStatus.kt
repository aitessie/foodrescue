package com.example.foodrescue.offerservice.domain.enum

enum class OfferStatus(
    val code: String,
    val description: String,
) {
    DRAFT("DRAFT", "Черновик"),
    SCHEDULED("SCHEDULED", "Запланировано"),
    ACTIVE("ACTIVE", "Активно"),
    SOLD_OUT("SOLD_OUT", "Распродано"),
    CLOSED("CLOSED", "Закрыто"),
    CANCELLED("CANCELLED", "Отменено"),
}
