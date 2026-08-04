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

@Component
class StoreRestMapper {

    fun toDomain(dto: StoreDto): Store {
        return Store(
            id = StoreId(dto.id),
            partnerId = PartnerId(dto.partnerId),
            name = dto.name,
            status = dto.status,
            address = Address(
                city = dto.address.city,
                street = dto.address.street,
                building = dto.address.building,
                postalCode = dto.address.postalCode,
            ),
            workingHours = dto.workingHours.map { workingHours ->
                WorkingHours(
                    dayOfWeek = workingHours.dayOfWeek,
                    opensAt = workingHours.opensAt,
                    closesAt = workingHours.closesAt,
                )
            },
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
        )
    }

    fun toDto(store: Store): StoreDto {
        return StoreDto(
            id = store.id.value,
            partnerId = store.partnerId.value,
            name = store.name,
            status = store.status,
            address = AddressDto(
                city = store.address.city,
                street = store.address.street,
                building = store.address.building,
                postalCode = store.address.postalCode,
            ),
            workingHours = store.workingHours.map { workingHours ->
                WorkingHoursDto(
                    dayOfWeek = workingHours.dayOfWeek,
                    opensAt = workingHours.opensAt,
                    closesAt = workingHours.closesAt,
                )
            },
            createdAt = store.createdAt,
            updatedAt = store.updatedAt,
        )
    }
}
