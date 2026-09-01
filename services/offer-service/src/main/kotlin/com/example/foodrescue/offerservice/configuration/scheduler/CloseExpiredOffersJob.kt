package com.example.foodrescue.offerservice.configuration.scheduler

import com.example.foodrescue.offerservice.application.usecases.CloseExpiredOffersUseCase
import com.example.foodrescue.offerservice.configuration.SchedulerProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "food-rescue.scheduler",
    name = ["close-expired-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CloseExpiredOffersJob(
    private val closeExpiredOffersUseCase: CloseExpiredOffersUseCase,
    private val schedulerProperties: SchedulerProperties,
) {
    @Scheduled(
        cron = "\${food-rescue.scheduler.close-expired-cron:0 */1 * * * *}",
        zone = "\${food-rescue.scheduler.close-expired-zone:UTC}",
    )
    fun closeExpiredOffers() {
        closeExpiredOffersUseCase.execute(schedulerProperties.closeExpiredBatchSize)
    }
}
