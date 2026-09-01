package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.ports.PublicOfferQueryPort
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferSearchItem
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetPublicOfferUseCase(
    private val publicOfferQueryPort: PublicOfferQueryPort,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun execute(offerId: OfferId): OfferSearchItem =
        publicOfferQueryPort.findVisibleById(
            offerId = offerId,
            visibleAt = clock.instant(),
        ) ?: throw OfferNotFoundException(offerId)
}
