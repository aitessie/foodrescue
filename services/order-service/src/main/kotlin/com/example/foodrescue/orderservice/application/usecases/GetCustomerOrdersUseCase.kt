package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.exceptions.OrderValidationException
import com.example.foodrescue.orderservice.application.ports.CurrentUserPort
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.domain.entities.OrderPage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetCustomerOrdersUseCase(
    private val orderDBPort: OrderDBPort,
    private val currentUserPort: CurrentUserPort,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun execute(
        page: Int,
        size: Int,
    ): OrderPage {
        val customerId = currentUserPort.getUserId()

        logger.info(
            "Trying to get customer orders: customerId={}, page={}, size={}",
            customerId,
            page,
            size,
        )

        validatePagination(
            page = page,
            size = size,
        )

        val result =
            orderDBPort.findByCustomerId(
                customerId = customerId,
                page = page,
                size = size,
            )

        logger.info(
            "Customer orders retrieved successfully: customerId={}, page={}, returnedCount={}",
            customerId,
            result.pageNumber,
            result.content.size,
        )

        return result
    }

    private fun validatePagination(
        page: Int,
        size: Int,
    ) {
        if (page !in MIN_PAGE..MAX_PAGE) {
            throw OrderValidationException("page must be between $MIN_PAGE and $MAX_PAGE")
        }
        if (size !in MIN_PAGE_SIZE..MAX_PAGE_SIZE) {
            throw OrderValidationException("size must be between $MIN_PAGE_SIZE and $MAX_PAGE_SIZE")
        }
    }

    private companion object {
        private const val MIN_PAGE = 0
        private const val MAX_PAGE = 100
        private const val MIN_PAGE_SIZE = 1
        private const val MAX_PAGE_SIZE = 100
    }
}
