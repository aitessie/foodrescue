package com.example.foodrescue.orderservice.adapter.`in`

import com.example.foodrescue.orderservice.adapter.`in`.dtos.CreateOrderDto
import com.example.foodrescue.orderservice.adapter.`in`.dtos.OrderDto
import com.example.foodrescue.orderservice.adapter.`in`.mappers.OrderRestMapper
import com.example.foodrescue.orderservice.application.usecases.CreateOrderUseCase
import com.example.foodrescue.orderservice.application.usecases.GetOrderUseCase
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val getOrderUseCase: GetOrderUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val orderRestMapper: OrderRestMapper,
) {
    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: UUID): OrderDto {
        val order = getOrderUseCase.execute(orderRestMapper.toOrderId(orderId))
        return orderRestMapper.toDto(order)
    }

    @PutMapping("/{orderId}")
    fun createOrder(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: CreateOrderDto,
    ): ResponseEntity<OrderDto> {
        val order =
            createOrderUseCase.execute(
                orderId = orderRestMapper.toOrderId(orderId),
                offerId = orderRestMapper.toOfferId(request.offerId),
                quantity = request.quantity,
            )

        return ResponseEntity.accepted().body(orderRestMapper.toDto(order))
    }
}
