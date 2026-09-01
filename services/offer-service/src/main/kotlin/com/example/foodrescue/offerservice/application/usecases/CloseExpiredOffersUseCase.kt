package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.exceptions.ValidationException
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CloseExpiredOffersUseCase(
    private val offerDBPort: OfferDBPort,
    private val processor: CloseExpiredOfferProcessor,
    private val clock: Clock,
) {
    fun execute(batchSize: Int): Int {
        validateBatchSize(batchSize)

        val now = clock.instant()
        val expiredOffers =
            offerDBPort.findExpiredBatch(
                expiredAt = now,
                batchSize = batchSize,
            )

        var closedOffers = 0

        expiredOffers.forEach { offer ->
            try {
                val closed =
                    processor.closeIfExpired(
                        offerId = offer.id,
                        now = now,
                    )

                if (closed) {
                    closedOffers += 1
                }
            } catch (exception: Exception) {
                logger.error(
                    "Failed to close expired offer {}",
                    offer.id.value,
                    exception,
                )
            }
        }

        return closedOffers
    }

    private fun validateBatchSize(batchSize: Int) {
        if (batchSize <= 0) {
            throw ValidationException("Expired-offer batchSize must be greater than zero")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CloseExpiredOffersUseCase::class.java)
    }
}
