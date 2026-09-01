package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.exceptions.InvalidSearchFilterException
import com.example.foodrescue.offerservice.application.ports.OfferSearchSettingsPort
import com.example.foodrescue.offerservice.application.ports.PublicOfferQueryPort
import com.example.foodrescue.offerservice.domain.entities.OfferSearchFilter
import com.example.foodrescue.offerservice.domain.entities.OfferSearchPage
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchOffersUseCase(
    private val publicOfferQueryPort: PublicOfferQueryPort,
    private val searchSettingsPort: OfferSearchSettingsPort,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun execute(filter: OfferSearchFilter): OfferSearchPage {
        validateFilter(filter)

        return publicOfferQueryPort.findVisiblePage(
            filter = filter,
            visibleAt = clock.instant(),
        )
    }

    private fun validateFilter(filter: OfferSearchFilter) {
        validatePage(filter.page)
        validateSize(filter.size)
    }

    private fun validatePage(page: Int) {
        if (page < 0) {
            throw InvalidSearchFilterException("page must not be negative")
        }
    }

    private fun validateSize(size: Int) {
        val minLimit = searchSettingsPort.minLimit
        val maxLimit = searchSettingsPort.maxLimit

        if (size < minLimit || size > maxLimit) {
            throw InvalidSearchFilterException("size must be between $minLimit and $maxLimit")
        }
    }
}
