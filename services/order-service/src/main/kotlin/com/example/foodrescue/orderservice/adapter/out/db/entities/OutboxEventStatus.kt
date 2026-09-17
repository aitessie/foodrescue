package com.example.foodrescue.orderservice.adapter.out.db.entities

enum class OutboxEventStatus {
    NEW,
    PUBLISHED,
    FAILED,
}
