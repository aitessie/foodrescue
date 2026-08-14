package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.enum.MoneyCurrency

data class Money(
    val amountMinor: Long,
    val currency: MoneyCurrency,
) {
    init {
        require(amountMinor > 0) {
            "Money amountMinor must be greater than zero"
        }
    }
}
