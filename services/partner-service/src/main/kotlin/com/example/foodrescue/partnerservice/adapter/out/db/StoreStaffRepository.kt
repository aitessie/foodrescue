package com.example.foodrescue.partnerservice.adapter.out.db

import com.example.foodrescue.partnerservice.adapter.out.db.persistence.StoreStaffJpaRepository
import com.example.foodrescue.partnerservice.application.ports.StoreStaffDBPort
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import org.springframework.stereotype.Repository

@Repository
class StoreStaffRepository(private val jpaRepository: StoreStaffJpaRepository) : StoreStaffDBPort {
    override fun isStaffAssignedToStore(
        userId: String,
        storeId: StoreId,
    ): Boolean =
        jpaRepository.existsByUserIdAndStoreId(
            userId = userId,
            storeId = storeId.value,
        )

    override fun isStaffAssignedToAnyStoreOfPartner(
        userId: String,
        partnerId: PartnerId,
    ): Boolean =
        jpaRepository.existsByUserIdAndPartnerId(
            userId = userId,
            partnerId = partnerId.value,
        )
}
