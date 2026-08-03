package com.example.foodrescue.partner.domain.enum

enum class PartnerStatus(
    val code: String,
    val description: String
) {
    ACTIVE("ACTIVE", "Активный"),
    SUSPENDED("SUSPENDED", "Приостановлен"),
}
