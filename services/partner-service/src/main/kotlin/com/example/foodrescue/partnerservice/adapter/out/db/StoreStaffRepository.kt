package com.example.foodrescue.partnerservice.adapter.out.db

import com.example.foodrescue.partnerservice.adapter.out.db.persistence.StoreStaffJpaRepository
import com.example.foodrescue.partnerservice.application.ports.StoreStaffDBPort
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import com.example.foodrescue.partnerservice.domain.entity.StoreId
import org.springframework.stereotype.Repository

@Repository
class StoreStaffRepository(
    private val jpaRepository: StoreStaffJpaRepository,
) : StoreStaffDBPort {

    override fun isStaffAssignedToStore(
        userId: String,
        storeId: StoreId,
    ): Boolean {
        return jpaRepository.existsByUserIdAndStoreId(
            userId = userId,
            storeId = storeId.value,
        )
    }

    override fun isStaffAssignedToAnyStoreOfPartner(
        userId: String,
        partnerId: PartnerId
    ): Boolean {
        return jpaRepository.existsByUserIdAndPartnerId(
            userId = userId,
            partnerId = partnerId.value,
        )
    }
}
