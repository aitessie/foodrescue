package com.example.foodrescue.orderservice.adapter.`in`.rest.dtos

class OrderPageDto(
    val items: List<OrderDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
