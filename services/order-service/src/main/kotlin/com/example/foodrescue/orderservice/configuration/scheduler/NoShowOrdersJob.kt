package com.example.foodrescue.orderservice.configuration.scheduler

import com.example.foodrescue.orderservice.application.usecases.MarkNoShowOrdersUseCase
import com.example.foodrescue.orderservice.configuration.OrderSchedulerProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NoShowOrdersJob(
    private val markNoShowOrdersUseCase: MarkNoShowOrdersUseCase,
    private val schedulerProperties: OrderSchedulerProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        cron = "\${food-rescue.scheduler.no-show-cron:0 */1 * * * *}",
        zone = "\${food-rescue.scheduler.no-show-zone:UTC}",
    )
    fun markNoShowOrders() {
        val markedOrders =
            markNoShowOrdersUseCase.execute(batchSize = schedulerProperties.noShowBatchSize)

        logger.info(
            "No-show Orders job completed: markedOrders={}",
            markedOrders,
        )
    }
}
