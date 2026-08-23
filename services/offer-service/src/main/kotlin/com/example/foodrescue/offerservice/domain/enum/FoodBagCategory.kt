package com.example.foodrescue.offerservice.domain.enum

enum class FoodBagCategory(
    val code: String,
    val description: String,
) {
    BAKERY("BAKERY", "Выпечка"),
    READY_MEAL("READY_MEAL", "Готовая еда"),
    GROCERY("GROCERY", "Бакалея"),
    FRUIT_AND_VEGETABLES(
        "FRUIT_AND_VEGETABLES",
        "Фрукты и овощи",
    ),
    OTHER("OTHER", "Другое"),
}
