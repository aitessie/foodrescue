package com.example.foodrescue.partnerservice.adapter.out.db.persistence

import com.example.foodrescue.partnerservice.adapter.out.db.entity.StoreStaffJpaEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StoreStaffJpaRepository : JpaRepository<StoreStaffJpaEntity, UUID> {
    fun existsByUserIdAndStoreId(
        userId: String,
        storeId: UUID,
    ): Boolean

    @Query(
        """
        select case when count(storeStaff) > 0 then true else false end
        from StoreStaffJpaEntity storeStaff
        where storeStaff.userId = :userId
          and storeStaff.storeId in (
              select store.id
              from StoreJpaEntity store
              where store.partnerId = :partnerId
          )
        """
    )
    fun existsByUserIdAndPartnerId(
        @Param("userId") userId: String,
        @Param("partnerId") partnerId: UUID,
    ): Boolean
}
