package com.example.foodrescue.offerservice.adapter.out.db

import com.example.foodrescue.offerservice.adapter.out.db.mappers.OfferJpaMapper
import com.example.foodrescue.offerservice.adapter.out.db.persistence.OfferJpaRepository
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import java.time.Instant
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class OfferRepository(
    private val offerJpaRepository: OfferJpaRepository,
    private val offerJpaMapper: OfferJpaMapper,
) : OfferDBPort {
    @Transactional(readOnly = true)
    override fun findById(id: OfferId): Offer? =
        offerJpaRepository.findById(id.value).orElse(null)?.let(offerJpaMapper::toDomain)

    @Transactional
    override fun save(offer: Offer): Offer {
        val entity = offerJpaMapper.toJpaEntity(offer)
        val savedEntity = offerJpaRepository.saveAndFlush(entity)

        return offerJpaMapper.toDomain(savedEntity)
    }

    @Transactional(readOnly = true)
    override fun findExpiredBatch(
        expiredAt: Instant,
        batchSize: Int,
    ): List<Offer> =
        offerJpaRepository
            .findExpiredBatch(
                statuses =
                    setOf(
                        OfferStatus.SCHEDULED,
                        OfferStatus.ACTIVE,
                    ),
                expiredAt = expiredAt,
                pageable =
                    PageRequest.of(
                        0,
                        batchSize,
                    ),
            )
            .map(offerJpaMapper::toDomain)
}
