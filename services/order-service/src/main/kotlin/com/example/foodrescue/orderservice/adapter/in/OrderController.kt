package com.example.foodrescue.orderservice.adapter.`in`

import com.example.foodrescue.orderservice.adapter.`in`.dtos.OrderDto
import com.example.foodrescue.orderservice.adapter.`in`.mappers.OrderRestMapper
import com.example.foodrescue.orderservice.application.usecases.GetOrderUseCase
import java.util.UUID
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val getOrderUseCase: GetOrderUseCase,
    private val orderRestMapper: OrderRestMapper,
) {
    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: UUID): OrderDto {
        val order = getOrderUseCase.execute(orderRestMapper.toOrderId(orderId))
        return orderRestMapper.toDto(order)
    }
}
