package com.example.foodrescue.offerservice.domain.entity

import java.util.UUID

@JvmInline
value class ProductTemplateId(val value: UUID) {
    override fun toString(): String = value.toString()
}
