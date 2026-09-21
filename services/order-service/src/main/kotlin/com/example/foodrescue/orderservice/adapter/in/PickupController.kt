package com.example.foodrescue.orderservice.adapter.`in`

import com.example.foodrescue.orderservice.adapter.`in`.dtos.ConfirmPickupDto
import com.example.foodrescue.orderservice.adapter.`in`.dtos.OrderDto
import com.example.foodrescue.orderservice.adapter.`in`.mappers.OrderRestMapper
import com.example.foodrescue.orderservice.application.usecases.ConfirmPickupUseCase
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/pickups")
class PickupController(
    private val confirmPickupUseCase: ConfirmPickupUseCase,
    private val orderRestMapper: OrderRestMapper,
) {
    @PostMapping("/confirm")
    fun confirmPickup(
        @Valid @RequestBody request: ConfirmPickupDto
    ): OrderDto =
        orderRestMapper.toDto(
            confirmPickupUseCase.execute(
                storeId = orderRestMapper.toStoreId(request.storeId),
                rawToken = request.token,
            )
        )
}
