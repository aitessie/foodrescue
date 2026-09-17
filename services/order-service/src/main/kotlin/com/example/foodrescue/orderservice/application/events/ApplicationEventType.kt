package com.example.foodrescue.orderservice.application.events

enum class ApplicationEventType(
    val code: String,
    val description: String,
) {
    ORDER_RESERVATION_REQUESTED(
        "order.reservation-requested",
        "Запрошено резервирование предложения для заказа",
    ),
}
