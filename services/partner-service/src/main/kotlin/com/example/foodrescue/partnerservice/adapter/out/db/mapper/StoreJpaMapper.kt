package com.example.foodrescue.partnerservice.adapter.out.db.mapper

import com.example.foodrescue.partnerservice.adapter.out.db.entity.AddressJpaEmbeddable
import com.example.foodrescue.partnerservice.adapter.out.db.entity.StoreJpaEntity
import com.example.foodrescue.partnerservice.adapter.out.db.entity.WorkingHoursJpaEmbeddable
import com.example.foodrescue.partnerservice.domain.entity.Address
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import com.example.foodrescue.partnerservice.domain.entity.Store
import com.example.foodrescue.partnerservice.domain.entity.StoreId
import com.example.foodrescue.partnerservice.domain.entity.WorkingHours
import org.springframework.stereotype.Component

@Component
class StoreJpaMapper {
    fun toEntity(store: Store): StoreJpaEntity =
        StoreJpaEntity(
            id = store.id.value,
            partnerId = store.partnerId.value,
            name = store.name,
            status = store.status,
            address =
                AddressJpaEmbeddable(
                    city = store.address.city,
                    street = store.address.street,
                    building = store.address.building,
                    postalCode = store.address.postalCode,
                ),
            workingHours =
                store.workingHours
                    .map { workingHours ->
                        WorkingHoursJpaEmbeddable(
                            dayOfWeek = workingHours.dayOfWeek,
                            opensAt = workingHours.opensAt,
                            closesAt = workingHours.closesAt,
                        )
                    }
                    .toMutableList(),
            createdAt = store.createdAt,
            updatedAt = store.updatedAt,
            version = store.version,
        )

    fun toDomain(entity: StoreJpaEntity): Store =
        Store(
            id = StoreId(entity.id),
            partnerId = PartnerId(entity.partnerId),
            name = entity.name,
            status = entity.status,
            address =
                Address(
                    city = entity.address.city,
                    street = entity.address.street,
                    building = entity.address.building,
                    postalCode = entity.address.postalCode,
                ),
            workingHours =
                entity.workingHours.map { workingHours ->
                    WorkingHours(
                        dayOfWeek = workingHours.dayOfWeek,
                        opensAt = workingHours.opensAt,
                        closesAt = workingHours.closesAt,
                    )
                },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            version = entity.version,
        )
}
