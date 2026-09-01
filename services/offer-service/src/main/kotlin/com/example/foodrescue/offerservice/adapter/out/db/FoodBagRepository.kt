package com.example.foodrescue.offerservice.adapter.out.db

import com.example.foodrescue.offerservice.adapter.`out`.db.mappers.FoodBagJpaMapper
import com.example.foodrescue.offerservice.adapter.out.db.persistence.FoodBagJpaRepository
import com.example.foodrescue.offerservice.application.ports.FoodBagDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class FoodBagRepository(
    private val foodBagJpaRepository: FoodBagJpaRepository,
    private val foodBagJpaMapper: FoodBagJpaMapper,
) : FoodBagDBPort {
    @Transactional(readOnly = true)
    override fun findById(id: FoodBagId): FoodBag? =
        foodBagJpaRepository.findById(id.value).orElse(null)?.let(foodBagJpaMapper::toDomain)

    @Transactional
    override fun save(foodBag: FoodBag): FoodBag {
        val entity = foodBagJpaMapper.toJpaEntity(foodBag)

        val savedEntity = foodBagJpaRepository.saveAndFlush(entity)

        return foodBagJpaMapper.toDomain(savedEntity)
    }
}
