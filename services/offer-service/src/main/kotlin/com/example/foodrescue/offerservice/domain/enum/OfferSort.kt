package com.example.foodrescue.offerservice.domain.enum

enum class OfferSort(
    val code: String,
    val description: String,
) {
    PICKUP_END_ASC(
        "PICKUP_END_ASC",
        "Сначала ближайшее окончание самовывоза",
    ),
    PRICE_ASC(
        "PRICE_ASC",
        "Сначала минимальная цена",
    ),
    DISTANCE_ASC(
        "DISTANCE_ASC",
        "Сначала минимальное расстояние",
    ),
}
