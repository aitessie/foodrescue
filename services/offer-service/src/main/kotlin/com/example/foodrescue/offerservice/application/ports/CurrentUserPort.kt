package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.enum.ApplicationRole

interface CurrentUserPort {
    fun getUserId(): String

    fun hasRole(role: ApplicationRole): Boolean
}
