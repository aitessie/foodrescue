package com.example.foodrescue.partnerservice.application.ports

import com.example.foodrescue.partnerservice.domain.enum.ApplicationRole

interface CurrentUserPort {
    fun getUserId(): String
    fun hasRole(role: ApplicationRole): Boolean
}
