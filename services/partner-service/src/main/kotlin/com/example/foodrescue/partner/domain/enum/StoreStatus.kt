package com.example.foodrescue.partner.domain.enum

enum class StoreStatus(
    val code: String,
    val description: String
) {
    ACTIVE("ACTIVE", "Активен"),
    SUSPENDED("SUSPENDED", "Приостановлен"),
}
