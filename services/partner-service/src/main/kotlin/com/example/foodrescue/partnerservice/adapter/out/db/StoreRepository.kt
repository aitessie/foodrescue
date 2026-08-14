package com.example.foodrescue.partnerservice.adapter.out.db

import com.example.foodrescue.partnerservice.adapter.out.db.mapper.StoreJpaMapper
import com.example.foodrescue.partnerservice.adapter.out.db.persistence.StoreJpaRepository
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import org.springframework.stereotype.Repository

@Repository
class StoreRepository(
    private val jpaRepository: StoreJpaRepository,
    private val mapper: StoreJpaMapper,
) : StoreDBPort {
    override fun save(store: Store): Store {
        val entity = mapper.toEntity(store)
        val savedEntity = jpaRepository.saveAndFlush(entity)

        return mapper.toDomain(savedEntity)
    }

    override fun findById(storeId: StoreId): Store? =
        jpaRepository.findById(storeId.value).map(mapper::toDomain).orElse(null)

    override fun findAllByPartnerId(partnerId: PartnerId): List<Store> =
        jpaRepository.findAllByPartnerId(partnerId.value).map(mapper::toDomain)
}
