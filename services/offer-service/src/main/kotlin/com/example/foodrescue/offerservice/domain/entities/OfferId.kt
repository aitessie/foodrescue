package com.example.foodrescue.offerservice.domain.entities

import java.util.UUID

@JvmInline
value class OfferId(val value: UUID) {
    override fun toString(): String = value.toString()
}
