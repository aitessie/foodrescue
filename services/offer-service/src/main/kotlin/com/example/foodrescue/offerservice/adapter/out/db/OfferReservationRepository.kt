package com.example.foodrescue.offerservice.adapter.out.db

import com.example.foodrescue.offerservice.adapter.`out`.db.mappers.OfferReservationJpaMapper
import com.example.foodrescue.offerservice.adapter.out.db.persistence.OfferReservationJpaRepository
import com.example.foodrescue.offerservice.application.ports.OfferReservationDBPort
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class OfferReservationRepository(
    private val offerReservationJpaRepository: OfferReservationJpaRepository,
    private val offerReservationJpaMapper: OfferReservationJpaMapper,
) : OfferReservationDBPort {
    @Transactional(readOnly = true)
    override fun findById(id: ReservationId): OfferReservation? =
        offerReservationJpaRepository
            .findById(id.value)
            .orElse(null)
            ?.let(offerReservationJpaMapper::toDomain)

    @Transactional
    override fun save(reservation: OfferReservation): OfferReservation {
        val entity = offerReservationJpaMapper.toJpaEntity(reservation)

        val savedEntity = offerReservationJpaRepository.saveAndFlush(entity)

        return offerReservationJpaMapper.toDomain(savedEntity)
    }
}
