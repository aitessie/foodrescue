package com.example.foodrescue.offerservice.domain.enum

enum class ApplicationRole(
    val code: String,
    val description: String,
) {
    CUSTOMER("CUSTOMER", "Покупатель"),
    STAFF("STAFF", "Сотрудник магазина"),
    MANAGER("MANAGER", "Менеджер партнёра"),
    ADMIN("ADMIN", "Администратор"),
}
