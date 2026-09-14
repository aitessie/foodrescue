package com.example.foodrescue.orderservice.application.ports

import com.example.foodrescue.orderservice.domain.enum.ApplicationRole

interface CurrentUserPort {
    fun getUserId(): String

    fun hasRole(role: ApplicationRole): Boolean
}
