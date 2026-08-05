package com.example.foodrescue.partnerservice.domain.entity

import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import java.time.Instant

class Partner(
    val id: PartnerId,
    managerId: String,
    name: String,
    status: PartnerStatus,
    val createdAt: Instant,
    updatedAt: Instant,
    val version: Long = 0,
) {

    val managerId: String = managerId.also {
        require(it.isNotBlank()) {
            "Partner managerId must not be blank"
        }
    }

    var name: String = name.trim().also {
        require(it.isNotBlank()) {
            "Partner name must not be blank"
        }
    }
        private set

    var status: PartnerStatus = status
        private set

    var updatedAt: Instant = updatedAt.also {
        require(!it.isBefore(createdAt)) {
            "Partner updatedAt must not be before createdAt"
        }
    }
        private set

    fun updateFrom(
        source: Partner,
        updatedAt: Instant,
    ) {
        require(source.name.isNotBlank()) {
            "Partner name must not be blank"
        }

        require(!updatedAt.isBefore(this.updatedAt)) {
            "New updatedAt must not be before current updatedAt"
        }

        name = source.name.trim()
        status = source.status
        this.updatedAt = updatedAt
    }
}
