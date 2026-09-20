package com.example.foodrescue.orderservice.adapter.`in`.dtos

class OrderPageDto(
    val items: List<OrderDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
