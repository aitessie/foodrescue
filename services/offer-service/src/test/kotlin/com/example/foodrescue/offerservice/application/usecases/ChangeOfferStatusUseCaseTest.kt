package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.OfferAccessPolicy
import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.offerservice.application.exceptions.FoodBagNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.FoodBagDBPort
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
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
class ChangeOfferStatusUseCaseTest {
    @Mock private lateinit var offerDBPort: OfferDBPort

    @Mock private lateinit var foodBagDBPort: FoodBagDBPort

    @Mock private lateinit var accessPolicy: OfferAccessPolicy

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: ChangeOfferStatusUseCase

    @Test
    fun whenScheduledOfferIsActivated_returnsSavedOffer() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)
        val foodBag =
            createFoodBag(
                id = offer.foodBagId,
                storeId = storeId,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedOffer =
            createOffer(
                id = offer.id,
                storeId = storeId,
                foodBagId = offer.foodBagId,
                status = OfferStatus.ACTIVE,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .offerStatusChanged(
                    offer = savedOffer,
                    occurredAt = now,
                )

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(foodBagDBPort.findById(offer.foodBagId)).thenReturn(foodBag)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerDBPort.save(offer)).thenReturn(savedOffer)
        `when`(
                eventFactory.offerStatusChanged(
                    offer = savedOffer,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                offerId = offer.id,
                targetStatus = OfferStatus.ACTIVE,
                expectedVersion = offer.version,
            )

        // Assert
        assertThat(result).isSameAs(savedOffer)
        assertThat(offer.status).isEqualTo(OfferStatus.ACTIVE)
        assertThat(offer.updatedAt).isEqualTo(now)

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(offer.foodBagId)
        verify(clock).instant()
        verify(offerDBPort).save(offer)
        verify(eventFactory)
            .offerStatusChanged(
                offer = savedOffer,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            accessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenScheduledOfferIsCancelled_returnsSavedOffer() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedOffer =
            createOffer(
                id = offer.id,
                storeId = storeId,
                foodBagId = offer.foodBagId,
                status = OfferStatus.CANCELLED,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .offerStatusChanged(
                    offer = savedOffer,
                    occurredAt = now,
                )

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerDBPort.save(offer)).thenReturn(savedOffer)
        `when`(
                eventFactory.offerStatusChanged(
                    offer = savedOffer,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                offerId = offer.id,
                targetStatus = OfferStatus.CANCELLED,
                expectedVersion = offer.version,
            )

        // Assert
        assertThat(result).isSameAs(savedOffer)
        assertThat(offer.status).isEqualTo(OfferStatus.CANCELLED)
        assertThat(offer.updatedAt).isEqualTo(now)

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(clock).instant()
        verify(offerDBPort).save(offer)
        verify(eventFactory)
            .offerStatusChanged(
                offer = savedOffer,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoInteractions(foodBagDBPort)
        verifyNoMoreInteractions(
            offerDBPort,
            accessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenRequestedStatusMatchesCurrentStatus_returnsExistingOffer() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                offerId = offer.id,
                targetStatus = OfferStatus.SCHEDULED,
                expectedVersion = offer.version,
            )

        // Assert
        assertThat(result).isSameAs(offer)

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verifyNoInteractions(
            foodBagDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            accessPolicy,
        )
    }

    @Test
    fun whenOfferDoesNotExist_throwsOfferNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())

        `when`(offerDBPort.findById(offerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offerId,
                    targetStatus = OfferStatus.ACTIVE,
                    expectedVersion = 0,
                )
            }

        // Assert
        assertThat(exception.message).contains(offerId.value.toString())

        verify(offerDBPort).findById(offerId)
        verifyNoInteractions(
            foodBagDBPort,
            accessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(offerDBPort)
    }

    @Test
    fun whenOfferBelongsToAnotherStore_throwsOfferNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer()

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offer.id,
                    targetStatus = OfferStatus.ACTIVE,
                    expectedVersion = offer.version,
                )
            }

        // Assert
        assertThat(exception.message).contains(offer.id.value.toString())

        verify(offerDBPort).findById(offer.id)
        verifyNoInteractions(
            foodBagDBPort,
            accessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(offerDBPort)
    }

    @Test
    fun whenUserCannotManageStore_throwsAccessDeniedException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)
        val expectedException = AccessDeniedException()

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        doThrow(expectedException)
            .`when`(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offer.id,
                    targetStatus = OfferStatus.ACTIVE,
                    expectedVersion = offer.version,
                )
            }

        // Assert
        assertThat(exception).isSameAs(expectedException)

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verifyNoInteractions(
            foodBagDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            accessPolicy,
        )
    }

    @Test
    fun whenExpectedVersionDoesNotMatchCurrentVersion_throwsEntityVersionConflictException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer =
            createOffer(
                storeId = storeId,
                version = 2,
            )
        val expectedVersion = 1L

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)

        // Act
        val exception =
            assertThrows<EntityVersionConflictException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offer.id,
                    targetStatus = OfferStatus.ACTIVE,
                    expectedVersion = expectedVersion,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo(
                "Version conflict for Offer '${offer.id.value}': " +
                    "supplied version $expectedVersion does not match current version ${offer.version}"
            )

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verifyNoInteractions(
            foodBagDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            accessPolicy,
        )
    }

    @Test
    fun whenFoodBagDoesNotExistForActivation_throwsFoodBagNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(foodBagDBPort.findById(offer.foodBagId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<FoodBagNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offer.id,
                    targetStatus = OfferStatus.ACTIVE,
                    expectedVersion = offer.version,
                )
            }

        // Assert
        assertThat(exception.message).contains(offer.foodBagId.value.toString())

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(offer.foodBagId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            accessPolicy,
        )
    }

    @Test
    fun whenFoodBagBelongsToAnotherStoreForActivation_throwsFoodBagNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)
        val foodBag = createFoodBag(id = offer.foodBagId)

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(foodBagDBPort.findById(offer.foodBagId)).thenReturn(foodBag)

        // Act
        val exception =
            assertThrows<FoodBagNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offer.id,
                    targetStatus = OfferStatus.ACTIVE,
                    expectedVersion = offer.version,
                )
            }

        // Assert
        assertThat(exception.message).contains(offer.foodBagId.value.toString())

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(offer.foodBagId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            accessPolicy,
        )
    }

    @Test
    fun whenInactiveFoodBagIsUsedForActivation_throwsInvalidStateException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)
        val foodBag =
            createFoodBag(
                id = offer.foodBagId,
                storeId = storeId,
                status = FoodBagStatus.INACTIVE,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(foodBagDBPort.findById(offer.foodBagId)).thenReturn(foodBag)
        `when`(clock.instant()).thenReturn(now)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offer.id,
                    targetStatus = OfferStatus.ACTIVE,
                    expectedVersion = offer.version,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Offer cannot become ACTIVE for an inactive FoodBag")
        assertThat(offer.status).isEqualTo(OfferStatus.SCHEDULED)

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(offer.foodBagId)
        verify(clock).instant()
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            accessPolicy,
            clock,
        )
    }

    @Test
    fun whenClosedStatusIsRequested_throwsInvalidStateException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(clock.instant()).thenReturn(now)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offer.id,
                    targetStatus = OfferStatus.CLOSED,
                    expectedVersion = offer.version,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("CLOSED status is managed by Offer Service")
        assertThat(offer.status).isEqualTo(OfferStatus.SCHEDULED)

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(clock).instant()
        verifyNoInteractions(
            foodBagDBPort,
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            accessPolicy,
            clock,
        )
    }

    private fun createOffer(
        id: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        foodBagId: FoodBagId = FoodBagId(UUID.randomUUID()),
        status: OfferStatus = OfferStatus.SCHEDULED,
        totalQuantity: Int = 5,
        availableQuantity: Int = 5,
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
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
            status = status,
            totalQuantity = totalQuantity,
            availableQuantity = availableQuantity,
            pickupWindow =
                PickupWindow(
                    start = Instant.parse("2026-08-20T12:00:00Z"),
                    end = Instant.parse("2026-08-20T14:00:00Z"),
                ),
            createdAt = Instant.parse("2026-08-20T10:00:00Z"),
            updatedAt = updatedAt,
            version = version,
        )

    private fun createFoodBag(
        id: FoodBagId = FoodBagId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        status: FoodBagStatus = FoodBagStatus.ACTIVE,
    ): FoodBag =
        FoodBag(
            id = id,
            storeId = storeId,
            name = "Surprise bag",
            description = "Food bag description",
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
            status = status,
            createdAt = Instant.parse("2026-08-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-08-20T10:00:00Z"),
            version = 0,
        )
}
