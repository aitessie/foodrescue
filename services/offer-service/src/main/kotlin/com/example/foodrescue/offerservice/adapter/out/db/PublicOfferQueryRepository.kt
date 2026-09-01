package com.example.foodrescue.offerservice.adapter.out.db

import com.example.foodrescue.offerservice.adapter.out.db.mappers.PublicOfferJpaMapper
import com.example.foodrescue.offerservice.adapter.out.db.persistence.PublicOfferJpaRepository
import com.example.foodrescue.offerservice.application.ports.PublicOfferQueryPort
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferSearchFilter
import com.example.foodrescue.offerservice.domain.entities.OfferSearchItem
import com.example.foodrescue.offerservice.domain.entities.OfferSearchPage
import com.example.foodrescue.offerservice.domain.enum.OfferStatus
import com.example.foodrescue.offerservice.domain.enum.PartnerStatus
import com.example.foodrescue.offerservice.domain.enum.StoreStatus
import java.time.Instant
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PublicOfferQueryRepository(
    private val publicOfferJpaRepository: PublicOfferJpaRepository,
    private val publicOfferJpaMapper: PublicOfferJpaMapper,
) : PublicOfferQueryPort {
    @Transactional(readOnly = true)
    override fun findVisibleById(
        offerId: OfferId,
        visibleAt: Instant,
    ): OfferSearchItem? =
        publicOfferJpaRepository
            .findVisibleOfferById(
                offerId = offerId.value,
                visibleAt = visibleAt,
                offerStatus = OfferStatus.ACTIVE,
                partnerStatus = PartnerStatus.ACTIVE,
                storeStatus = StoreStatus.ACTIVE,
            )
            ?.let(publicOfferJpaMapper::toDomain)

    @Transactional(readOnly = true)
    override fun findVisiblePage(
        filter: OfferSearchFilter,
        visibleAt: Instant,
    ): OfferSearchPage {
        val result =
            publicOfferJpaRepository.findVisibleOffers(
                storeId = filter.storeId?.value,
                category = filter.category,
                visibleAt = visibleAt,
                offerStatus = OfferStatus.ACTIVE,
                partnerStatus = PartnerStatus.ACTIVE,
                storeStatus = StoreStatus.ACTIVE,
                pageable =
                    PageRequest.of(
                        filter.page,
                        filter.size,
                    ),
            )

        return OfferSearchPage(
            content = result.content.map(publicOfferJpaMapper::toDomain),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            pageNumber = result.number,
            pageSize = result.size,
        )
    }
}
