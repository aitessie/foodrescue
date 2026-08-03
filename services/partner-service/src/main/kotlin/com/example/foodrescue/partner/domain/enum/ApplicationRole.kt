package com.example.foodrescue.partner.domain.enum

enum class ApplicationRole(
    val code: String,
    val description: String
) {

    CUSTOMER("CUSTOMER", "Покупатель"),
    STAFF("STAFF", "Персонал магазина"),
    MANAGER("MANAGER", "Мэнеджер магазина"),
    ADMIN("ADMIN", "Админ"),
}
