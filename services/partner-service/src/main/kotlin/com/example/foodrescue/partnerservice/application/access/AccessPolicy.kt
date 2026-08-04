package com.example.foodrescue.partnerservice.application.access

import com.example.foodrescue.partnerservice.domain.enum.AccessAction

interface AccessPolicy <T> {

    fun checkAccess(
        action: AccessAction,
        resource: T,
    )
}
