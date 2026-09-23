package com.example.foodrescue.orderservice.application.usecases

import com.example.foodrescue.orderservice.application.exceptions.OrderValidationException
import com.example.foodrescue.orderservice.application.ports.CurrentUserPort
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.domain.entities.OrderPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GetCustomerOrdersUseCaseTest {
    @Mock private lateinit var orderDBPort: OrderDBPort

    @Mock private lateinit var currentUserPort: CurrentUserPort

    @InjectMocks private lateinit var useCase: GetCustomerOrdersUseCase

    @Test
    fun whenCustomerOrdersExist_returnsOrderPage() {
        // Arrange
        val page = 2
        val size = 20
        val orderPage =
            OrderPage(
                content = emptyList(),
                totalElements = 45,
                totalPages = 3,
                pageNumber = page,
                pageSize = size,
            )

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                orderDBPort.findByCustomerId(
                    customerId = CURRENT_USER_ID,
                    page = page,
                    size = size,
                )
            )
            .thenReturn(orderPage)

        // Act
        val result =
            useCase.execute(
                page = page,
                size = size,
            )

        // Assert
        assertThat(result).isSameAs(orderPage)

        verify(currentUserPort).getUserId()
        verify(orderDBPort)
            .findByCustomerId(
                customerId = CURRENT_USER_ID,
                page = page,
                size = size,
            )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
        )
    }

    @ParameterizedTest
    @CsvSource(
        "0, 1",
        "100, 100",
    )
    fun whenPaginationIsOnAllowedBoundary_returnsOrderPage(
        page: Int,
        size: Int,
    ) {
        // Arrange
        val orderPage =
            OrderPage(
                content = emptyList(),
                totalElements = 0,
                totalPages = 0,
                pageNumber = page,
                pageSize = size,
            )

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                orderDBPort.findByCustomerId(
                    customerId = CURRENT_USER_ID,
                    page = page,
                    size = size,
                )
            )
            .thenReturn(orderPage)

        // Act
        val result =
            useCase.execute(
                page = page,
                size = size,
            )

        // Assert
        assertThat(result).isSameAs(orderPage)

        verify(currentUserPort).getUserId()
        verify(orderDBPort)
            .findByCustomerId(
                customerId = CURRENT_USER_ID,
                page = page,
                size = size,
            )
        verifyNoMoreInteractions(
            orderDBPort,
            currentUserPort,
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 101])
    fun whenPageIsOutsideAllowedRange_throwsOrderValidationException(page: Int) {
        // Arrange
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)

        // Act
        val exception =
            assertThrows<OrderValidationException> {
                useCase.execute(
                    page = page,
                    size = 20,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("page must be between 0 and 100")

        verify(currentUserPort).getUserId()
        verifyNoInteractions(orderDBPort)
        verifyNoMoreInteractions(currentUserPort)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 101])
    fun whenSizeIsOutsideAllowedRange_throwsOrderValidationException(size: Int) {
        // Arrange
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)

        // Act
        val exception =
            assertThrows<OrderValidationException> {
                useCase.execute(
                    page = 0,
                    size = size,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("size must be between 1 and 100")

        verify(currentUserPort).getUserId()
        verifyNoInteractions(orderDBPort)
        verifyNoMoreInteractions(currentUserPort)
    }

    companion object {
        private const val CURRENT_USER_ID = "33333333-3333-3333-3333-333333333333"
    }
}
