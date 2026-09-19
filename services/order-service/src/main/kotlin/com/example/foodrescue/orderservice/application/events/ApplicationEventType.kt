package com.example.foodrescue.orderservice.application.events

enum class ApplicationEventType(
    val code: String,
    val description: String,
) {
    ORDER_RESERVATION_REQUESTED(
        "order.reservation-requested",
        "Запрошено резервирование предложения для заказа",
    ),
    ORDER_RESERVATION_COMMIT_REQUESTED(
        "order.reservation-commit-requested",
        "Запрошено подтверждение резервирования предложения для заказа",
    ),
    ORDER_RESERVATION_RELEASE_REQUESTED(
        "order.reservation-release-requested",
        "Запрошено освобождение резервирования предложения для заказа",
    ),
}
