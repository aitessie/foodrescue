package com.example.foodrescue.partner.domain.entity

import com.example.foodrescue.partner.domain.enum.PartnerStatus
import kotlin.time.Clock
import kotlin.time.Instant

class Partner(
    val id: PartnerId,
    val managerId: String,
    name: String,
    status: PartnerStatus,
    val createdAt: Instant,
    updatedAt: Instant,
) {
    var name: String = name
        private set

    var status: PartnerStatus = status
        private set

    var updatedAt: Instant = updatedAt
        private set

    fun updateFrom(source: Partner) {
        require(source.name.isNotBlank()) {
            "Partner name must not be blank"
        }

        name = source.name.trim()
        status = source.status
        updatedAt = Clock.System.now()
    }
}
