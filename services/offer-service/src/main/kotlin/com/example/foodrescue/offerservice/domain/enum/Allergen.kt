package com.example.foodrescue.offerservice.domain.enum

enum class Allergen(
    val code: String,
    val description: String,
) {
    GLUTEN("GLUTEN", "Глютен"),
    MILK("MILK", "Молоко"),
    EGGS("EGGS", "Яйца"),
    NUTS("NUTS", "Орехи"),
    PEANUTS("PEANUTS", "Арахис"),
    SOY("SOY", "Соя"),
    FISH("FISH", "Рыба"),
    SHELLFISH("SHELLFISH", "Ракообразные и моллюски"),
    SESAME("SESAME", "Кунжут"),
}
