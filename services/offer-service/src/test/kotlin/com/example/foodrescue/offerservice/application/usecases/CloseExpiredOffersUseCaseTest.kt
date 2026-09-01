package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.exceptions.ValidationException
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CloseExpiredOffersUseCaseTest {
    @Mock private lateinit var offerDBPort: OfferDBPort

    @Mock private lateinit var processor: CloseExpiredOfferProcessor

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: CloseExpiredOffersUseCase

    @Test
    fun whenExpiredOffersAreClosed_returnsClosedOffersCount() {
        // Arrange
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val firstOffer = createOffer()
        val secondOffer = createOffer()
        val batchSize = 10

        `when`(clock.instant()).thenReturn(now)
        `when`(
                offerDBPort.findExpiredBatch(
                    expiredAt = now,
                    batchSize = batchSize,
                )
            )
            .thenReturn(listOf(firstOffer, secondOffer))
        `when`(
                processor.closeIfExpired(
                    offerId = firstOffer.id,
                    now = now,
                )
            )
            .thenReturn(true)
        `when`(
                processor.closeIfExpired(
                    offerId = secondOffer.id,
                    now = now,
                )
            )
            .thenReturn(true)

        // Act
        val result = useCase.execute(batchSize = batchSize)

        // Assert
        assertThat(result).isEqualTo(2)

        verify(clock).instant()
        verify(offerDBPort)
            .findExpiredBatch(
                expiredAt = now,
                batchSize = batchSize,
            )
        verify(processor)
            .closeIfExpired(
                offerId = firstOffer.id,
                now = now,
            )
        verify(processor)
            .closeIfExpired(
                offerId = secondOffer.id,
                now = now,
            )
        verifyNoMoreInteractions(
            offerDBPort,
            processor,
            clock,
        )
    }

    @Test
    fun whenSomeExpiredOffersAreNotClosed_returnsOnlyClosedOffersCount() {
        // Arrange
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val firstOffer = createOffer()
        val secondOffer = createOffer()
        val batchSize = 10

        `when`(clock.instant()).thenReturn(now)
        `when`(
                offerDBPort.findExpiredBatch(
                    expiredAt = now,
                    batchSize = batchSize,
                )
            )
            .thenReturn(listOf(firstOffer, secondOffer))
        `when`(
                processor.closeIfExpired(
                    offerId = firstOffer.id,
                    now = now,
                )
            )
            .thenReturn(true)
        `when`(
                processor.closeIfExpired(
                    offerId = secondOffer.id,
                    now = now,
                )
            )
            .thenReturn(false)

        // Act
        val result = useCase.execute(batchSize = batchSize)

        // Assert
        assertThat(result).isEqualTo(1)

        verify(clock).instant()
        verify(offerDBPort)
            .findExpiredBatch(
                expiredAt = now,
                batchSize = batchSize,
            )
        verify(processor)
            .closeIfExpired(
                offerId = firstOffer.id,
                now = now,
            )
        verify(processor)
            .closeIfExpired(
                offerId = secondOffer.id,
                now = now,
            )
        verifyNoMoreInteractions(
            offerDBPort,
            processor,
            clock,
        )
    }

    @Test
    fun whenClosingExpiredOfferFails_continuesProcessingRemainingOffers() {
        // Arrange
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val firstOffer = createOffer()
        val secondOffer = createOffer()
        val batchSize = 10

        `when`(clock.instant()).thenReturn(now)
        `when`(
                offerDBPort.findExpiredBatch(
                    expiredAt = now,
                    batchSize = batchSize,
                )
            )
            .thenReturn(listOf(firstOffer, secondOffer))
        `when`(
                processor.closeIfExpired(
                    offerId = firstOffer.id,
                    now = now,
                )
            )
            .thenThrow(RuntimeException("Failed to close offer"))
        `when`(
                processor.closeIfExpired(
                    offerId = secondOffer.id,
                    now = now,
                )
            )
            .thenReturn(true)

        // Act
        val result = useCase.execute(batchSize = batchSize)

        // Assert
        assertThat(result).isEqualTo(1)

        verify(clock).instant()
        verify(offerDBPort)
            .findExpiredBatch(
                expiredAt = now,
                batchSize = batchSize,
            )
        verify(processor)
            .closeIfExpired(
                offerId = firstOffer.id,
                now = now,
            )
        verify(processor)
            .closeIfExpired(
                offerId = secondOffer.id,
                now = now,
            )
        verifyNoMoreInteractions(
            offerDBPort,
            processor,
            clock,
        )
    }

    @Test
    fun whenNoExpiredOffersAreFound_returnsZero() {
        // Arrange
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val batchSize = 10

        `when`(clock.instant()).thenReturn(now)
        `when`(
                offerDBPort.findExpiredBatch(
                    expiredAt = now,
                    batchSize = batchSize,
                )
            )
            .thenReturn(emptyList())

        // Act
        val result = useCase.execute(batchSize = batchSize)

        // Assert
        assertThat(result).isZero()

        verify(clock).instant()
        verify(offerDBPort)
            .findExpiredBatch(
                expiredAt = now,
                batchSize = batchSize,
            )
        verifyNoInteractions(processor)
        verifyNoMoreInteractions(
            offerDBPort,
            clock,
        )
    }

    @Test
    fun whenBatchSizeIsNotPositive_throwsValidationException() {
        // Arrange
        val batchSize = 0

        // Act
        val exception =
            assertThrows<ValidationException> {
                useCase.execute(batchSize = batchSize)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Expired-offer batchSize must be greater than zero")

        verifyNoInteractions(
            offerDBPort,
            processor,
            clock,
        )
    }

    private fun createOffer(
        id: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        foodBagId: FoodBagId = FoodBagId(UUID.randomUUID()),
    ): Offer =
        Offer(
            id = id,
            storeId = storeId,
            foodBagId = foodBagId,
            category = FoodBagCategory.entries.first(),
            unitPrice =
                Money(
                    amountMinor = 500,
                    currency = MoneyCurrency.RUB,
                ),
            allergens = emptySet(),
            status = OfferStatus.ACTIVE,
            totalQuantity = 5,
            availableQuantity = 5,
            pickupWindow =
                PickupWindow(
                    start = Instant.parse("2026-08-20T09:00:00Z"),
                    end = Instant.parse("2026-08-20T10:30:00Z"),
                ),
            createdAt = Instant.parse("2026-08-20T09:00:00Z"),
            updatedAt = Instant.parse("2026-08-20T10:00:00Z"),
            version = 0,
        )
}
