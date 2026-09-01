package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.ports.PublicOfferQueryPort
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferSearchItem
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
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
class GetPublicOfferUseCaseTest {
    @Mock private lateinit var publicOfferQueryPort: PublicOfferQueryPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: GetPublicOfferUseCase

    @Test
    fun whenVisibleOfferExists_returnsOffer() {
        // Arrange
        val offer = createOfferSearchItem()
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                publicOfferQueryPort.findVisibleById(
                    offerId = offer.offerId,
                    visibleAt = now,
                )
            )
            .thenReturn(offer)

        // Act
        val result = useCase.execute(offerId = offer.offerId)

        // Assert
        assertThat(result).isSameAs(offer)

        verify(clock).instant()
        verify(publicOfferQueryPort)
            .findVisibleById(
                offerId = offer.offerId,
                visibleAt = now,
            )
        verifyNoMoreInteractions(
            publicOfferQueryPort,
            clock,
        )
    }

    @Test
    fun whenVisibleOfferDoesNotExist_throwsOfferNotFoundException() {
        // Arrange
        val offerId = OfferId(UUID.randomUUID())
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(clock.instant()).thenReturn(now)
        `when`(
                publicOfferQueryPort.findVisibleById(
                    offerId = offerId,
                    visibleAt = now,
                )
            )
            .thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(offerId = offerId)
            }

        // Assert
        assertThat(exception.message).contains(offerId.value.toString())

        verify(clock).instant()
        verify(publicOfferQueryPort)
            .findVisibleById(
                offerId = offerId,
                visibleAt = now,
            )
        verifyNoMoreInteractions(
            publicOfferQueryPort,
            clock,
        )
    }

    private fun createOfferSearchItem(
        offerId: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        foodBagId: FoodBagId = FoodBagId(UUID.randomUUID()),
    ): OfferSearchItem =
        OfferSearchItem(
            offerId = offerId,
            storeId = storeId,
            foodBagId = foodBagId,
            foodBagName = "Surprise bag",
            foodBagDescription = "Food bag description",
            category = FoodBagCategory.entries.first(),
            originalPrice =
                Money(
                    amountMinor = 1000,
                    currency = MoneyCurrency.RUB,
                ),
            unitPrice =
                Money(
                    amountMinor = 500,
                    currency = MoneyCurrency.RUB,
                ),
            allergens = emptySet(),
            availableQuantity = 5,
            pickupWindow =
                PickupWindow(
                    start = Instant.parse("2026-08-20T12:00:00Z"),
                    end = Instant.parse("2026-08-20T14:00:00Z"),
                ),
            storeName = "Test store",
            storeAddress = "Test address",
            storeTimeZone = ZoneId.of("Europe/Moscow"),
        )
}
