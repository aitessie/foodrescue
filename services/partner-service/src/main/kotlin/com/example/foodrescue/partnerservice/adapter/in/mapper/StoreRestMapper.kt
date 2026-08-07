package com.example.foodrescue.partnerservice.adapter.`in`.mapper

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.AddressDto
import com.example.foodrescue.partnerservice.adapter.`in`.dtos.StoreDto
import com.example.foodrescue.partnerservice.adapter.`in`.dtos.WorkingHoursDto
import com.example.foodrescue.partnerservice.domain.entity.Address
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import com.example.foodrescue.partnerservice.domain.entity.Store
import com.example.foodrescue.partnerservice.domain.entity.StoreId
import com.example.foodrescue.partnerservice.domain.entity.WorkingHours
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class StoreRestMapper(
    private val clock: Clock,
) {

    fun toDomain(
        dto: StoreDto,
        partnerId: PartnerId,
        storeId: StoreId,
    ): Store {
        val now = Instant.now(clock)

        return Store(
            id = storeId,
            partnerId = partnerId,
            name = dto.name,
            status = dto.status,
            workingHours = dto.workingHours.map(::toDomain),
            address = toDomain(dto.address),
            createdAt = now,
            updatedAt = now,
            version = dto.version,
        )
    }

    fun toDto(store: Store): StoreDto {
        return StoreDto(
            name = store.name,
            status = store.status,
            workingHours = store.workingHours.map(::toDto),
            address = toDto(store.address),
            version = store.version,
        )
    }

    private fun toDomain(dto: WorkingHoursDto): WorkingHours {
        return WorkingHours(
            dayOfWeek = dto.dayOfWeek,
            opensAt = dto.opensAt,
            closesAt = dto.closesAt,
        )
    }

    private fun toDto(workingHours: WorkingHours): WorkingHoursDto {
        return WorkingHoursDto(
            dayOfWeek = workingHours.dayOfWeek,
            opensAt = workingHours.opensAt,
            closesAt = workingHours.closesAt,
        )
    }

    private fun toDomain(dto: AddressDto): Address {
        return Address(
            city = dto.city,
            street = dto.street,
            building = dto.building,
            postalCode = dto.postalCode,
        )
    }

    private fun toDto(address: Address): AddressDto {
        return AddressDto(
            city = address.city,
            street = address.street,
            building = address.building,
            postalCode = address.postalCode,
        )
    }
}
