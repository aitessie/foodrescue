package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.exceptions.InvalidSearchFilterException
import com.example.foodrescue.offerservice.application.ports.OfferSearchSettingsPort
import com.example.foodrescue.offerservice.application.ports.PublicOfferQueryPort
import com.example.foodrescue.offerservice.domain.entities.OfferSearchFilter
import com.example.foodrescue.offerservice.domain.entities.OfferSearchPage
import java.time.Clock
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SearchOffersUseCaseTest {
    @Mock private lateinit var publicOfferQueryPort: PublicOfferQueryPort

    @Mock private lateinit var searchSettingsPort: OfferSearchSettingsPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: SearchOffersUseCase

    @ParameterizedTest
    @ValueSource(ints = [1, 100])
    fun whenSizeIsAtAllowedBoundary_returnsSearchPage(size: Int) {
        // Arrange
        val filter = createFilter(size = size)
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val page =
            OfferSearchPage(
                content = emptyList(),
                totalElements = 0,
                totalPages = 0,
                pageNumber = filter.page,
                pageSize = filter.size,
            )

        `when`(searchSettingsPort.minLimit).thenReturn(MIN_LIMIT)
        `when`(searchSettingsPort.maxLimit).thenReturn(MAX_LIMIT)
        `when`(clock.instant()).thenReturn(now)
        `when`(
                publicOfferQueryPort.findVisiblePage(
                    filter = filter,
                    visibleAt = now,
                )
            )
            .thenReturn(page)

        // Act
        val result = useCase.execute(filter = filter)

        // Assert
        assertThat(result).isSameAs(page)

        verify(searchSettingsPort).minLimit
        verify(searchSettingsPort).maxLimit
        verify(clock).instant()
        verify(publicOfferQueryPort)
            .findVisiblePage(
                filter = filter,
                visibleAt = now,
            )
        verifyNoMoreInteractions(
            publicOfferQueryPort,
            searchSettingsPort,
            clock,
        )
    }

    @Test
    fun whenPageIsNegative_throwsInvalidSearchFilterException() {
        // Arrange
        val filter = createFilter(page = -1)

        // Act
        val exception =
            assertThrows<InvalidSearchFilterException> {
                useCase.execute(filter = filter)
            }

        // Assert
        assertThat(exception.message).isEqualTo("page must not be negative")

        verifyNoInteractions(
            publicOfferQueryPort,
            searchSettingsPort,
            clock,
        )
    }

    @Test
    fun whenSizeIsBelowMinimum_throwsInvalidSearchFilterException() {
        // Arrange
        val filter = createFilter(size = MIN_LIMIT - 1)

        `when`(searchSettingsPort.minLimit).thenReturn(MIN_LIMIT)
        `when`(searchSettingsPort.maxLimit).thenReturn(MAX_LIMIT)

        // Act
        val exception =
            assertThrows<InvalidSearchFilterException> {
                useCase.execute(filter = filter)
            }

        // Assert
        assertThat(exception.message).isEqualTo("size must be between $MIN_LIMIT and $MAX_LIMIT")

        verify(searchSettingsPort).minLimit
        verify(searchSettingsPort).maxLimit
        verifyNoInteractions(
            publicOfferQueryPort,
            clock,
        )
        verifyNoMoreInteractions(searchSettingsPort)
    }

    @Test
    fun whenSizeIsAboveMaximum_throwsInvalidSearchFilterException() {
        // Arrange
        val filter = createFilter(size = MAX_LIMIT + 1)

        `when`(searchSettingsPort.minLimit).thenReturn(MIN_LIMIT)
        `when`(searchSettingsPort.maxLimit).thenReturn(MAX_LIMIT)

        // Act
        val exception =
            assertThrows<InvalidSearchFilterException> {
                useCase.execute(filter = filter)
            }

        // Assert
        assertThat(exception.message).isEqualTo("size must be between $MIN_LIMIT and $MAX_LIMIT")

        verify(searchSettingsPort).minLimit
        verify(searchSettingsPort).maxLimit
        verifyNoInteractions(
            publicOfferQueryPort,
            clock,
        )
        verifyNoMoreInteractions(searchSettingsPort)
    }

    private fun createFilter(
        page: Int = 0,
        size: Int = 20,
    ): OfferSearchFilter =
        OfferSearchFilter(
            storeId = null,
            category = null,
            page = page,
            size = size,
        )

    companion object {
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 100
    }
}
