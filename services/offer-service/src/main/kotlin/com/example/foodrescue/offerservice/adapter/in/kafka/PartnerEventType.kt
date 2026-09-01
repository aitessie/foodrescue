package com.example.foodrescue.offerservice.adapter.`in`.kafka

enum class PartnerEventType(
    val code: String,
    val description: String,
) {
    STORE_CREATED(
        code = "store.created",
        description = "Магазин создан",
    ),
    STORE_UPDATED(
        code = "store.updated",
        description = "Магазин обновлён",
    ),
    STORE_SUSPENDED(
        code = "store.suspended",
        description = "Магазин приостановлен",
    ),
    PARTNER_UPDATED(
        code = "partner.updated",
        description = "Партнёр обновлён",
    ),
    PARTNER_SUSPENDED(
        code = "partner.suspended",
        description = "Партнёр приостановлен",
    ),
}
