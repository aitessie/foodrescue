package com.example.foodrescue.partnerservice.domain.enum

enum class ApplicationRole(
    val code: String,
    val description: String
) {

    CUSTOMER("CUSTOMER", "Покупатель"),
    STAFF("STAFF", "Персонал магазина"),
    MANAGER("MANAGER", "Мэнеджер магазина"),
    ADMIN("ADMIN", "Админ"),
}
