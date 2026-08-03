package com.example.foodrescue.partner.domain.entity

import com.example.foodrescue.partner.domain.enum.StoreStatus
import kotlin.time.Clock
import kotlin.time.Instant

class Store (
    val id: StoreId,
    val partnerId: PartnerId,
    name: String,
    status: StoreStatus,
    workingHours: List<WorkingHours>,
    address: Address,
    val createdAt: Instant,
    updatedAt: Instant,
){
    var name: String = name
        private set

    var address: Address = address
        private set

    var status: StoreStatus = status
        private set

    var workingHours: List<WorkingHours> = workingHours.toList()
        private set

    var updatedAt: Instant = updatedAt
        private set

    fun updateFrom(source: Store) {
        require(source.name.isNotBlank()) {
            "Store name must not be blank"
        }

        name = source.name.trim()
        address = source.address
        status = source.status
        workingHours = source.workingHours.toList()
        updatedAt = Clock.System.now()
    }
}
