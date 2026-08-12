package com.example.foodrescue.offerservice.domain.entity

import java.util.UUID

@JvmInline
value class StoreId(val value: UUID) {
    override fun toString(): String = value.toString()
}
