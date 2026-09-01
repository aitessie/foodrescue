package com.example.foodrescue.offerservice.application.events

enum class ApplicationEventType(
    val code: String,
    val description: String,
) {
    FOOD_BAG_CREATED(
        "food-bag.created",
        "Набор еды создан",
    ),
    FOOD_BAG_UPDATED(
        "food-bag.updated",
        "Набор еды обновлён",
    ),
    FOOD_BAG_STATUS_CHANGED(
        "food-bag.status-changed",
        "Статус набора еды изменён",
    ),
    OFFER_CREATED(
        "offer.created",
        "Предложение создано",
    ),
    OFFER_UPDATED(
        "offer.updated",
        "Предложение обновлено",
    ),
    OFFER_STATUS_CHANGED(
        "offer.status-changed",
        "Статус предложения изменён",
    ),
    OFFER_QUANTITY_CHANGED(
        "offer.quantity-changed",
        "Общее количество предложения изменено",
    ),
    OFFER_RESERVED(
        "offer.reserved",
        "Наборы еды зарезервированы",
    ),
    OFFER_RESERVATION_RELEASED(
        "offer.reservation-released",
        "Резервирование наборов еды освобождено",
    ),
    OFFER_CLOSED(
        "offer.closed",
        "Предложение закрыто",
    ),
}
