package com.example.foodrescue.partnerservice.domain.entity

import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
import java.time.Instant

class Store (
    val id: StoreId,
    val partnerId: PartnerId,
    name: String,
    status: StoreStatus,
    workingHours: List<WorkingHours>,
    address: Address,
    var createdAt: Instant,
    updatedAt: Instant,
    val version: Long = 0,
){
    var name: String = name.trim().also {
        require(it.isNotBlank()) {
            "Store name must not be blank"
        }
    }
        private set

    var address: Address = address
        private set

    var status: StoreStatus = status
        private set

    var workingHours: List<WorkingHours> = workingHours.toList()
        private set

    var updatedAt: Instant = updatedAt.also {
        require(!it.isBefore(createdAt)) {
            "Partner updatedAt must not be before createdAt"
        }
    }
        private set

    fun updateFrom(
        source: Store,
        updatedAt: Instant,
    ) {
        require(source.name.isNotBlank()) {
            "Store name must not be blank"
        }

        require(!updatedAt.isBefore(this.updatedAt)) {
            "New updatedAt must not be before current updatedAt"
        }

        name = source.name.trim()
        address = source.address
        status = source.status
        workingHours = source.workingHours.toList()
        this.updatedAt = updatedAt
    }
}
