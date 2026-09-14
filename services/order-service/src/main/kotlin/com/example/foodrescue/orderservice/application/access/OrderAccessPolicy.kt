package com.example.foodrescue.orderservice.application.access

import com.example.foodrescue.orderservice.application.exceptions.OrderAccessDeniedException
import com.example.foodrescue.orderservice.application.ports.CurrentUserPort
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.enum.ApplicationRole
import org.springframework.stereotype.Service

@Service
class OrderAccessPolicy(private val currentUserPort: CurrentUserPort) {
    fun checkReadAccess(order: Order) {
        val userId = currentUserPort.getUserId()

        val accessAllowed =
            currentUserPort.hasRole(ApplicationRole.ADMIN) ||
                (currentUserPort.hasRole(ApplicationRole.CUSTOMER) && order.customerId == userId)

        if (!accessAllowed) {
            throw OrderAccessDeniedException()
        }
    }
}
