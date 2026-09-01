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
class CreateOrUpdateOfferUseCaseTest {
    @Mock private lateinit var offerDBPort: OfferDBPort

    @Mock private lateinit var foodBagDBPort: FoodBagDBPort

    @Mock private lateinit var offerAccessPolicy: OfferAccessPolicy

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: CreateOrUpdateOfferUseCase

    @Test
    fun whenNewOfferIsCreated_returnsSavedOffer() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val foodBag = createFoodBag(storeId = storeId)
        val totalQuantity = 7
        val pickupWindow =
            PickupWindow(
                start = Instant.parse("2026-08-20T12:00:00Z"),
                end = Instant.parse("2026-08-20T14:00:00Z"),
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val event =
            ApplicationEventFactory()
                .offerCreated(
                    offer =
                        createOffer(
                            id = offerId,
                            storeId = storeId,
                            foodBagId = foodBag.id,
                            totalQuantity = totalQuantity,
                            availableQuantity = totalQuantity,
                            pickupWindow = pickupWindow,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    occurredAt = now,
                )

        `when`(offerDBPort.findById(offerId)).thenReturn(null)
        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)
        `when`(clock.instant()).thenReturn(now)
        doAnswer { invocation -> invocation.getArgument<Offer>(0) }
            .`when`(offerDBPort)
            .save(anyOffer())
        doReturn(event)
            .`when`(eventFactory)
            .offerCreated(
                offer = anyOffer(),
                occurredAt = anyInstant(),
            )

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                offerId = offerId,
                foodBagId = foodBag.id,
                totalQuantity = totalQuantity,
                pickupWindow = pickupWindow,
                requestedVersion = 0,
            )

        // Assert
        assertThat(result.id).isEqualTo(offerId)
        assertThat(result.storeId).isEqualTo(storeId)
        assertThat(result.foodBagId).isEqualTo(foodBag.id)
        assertThat(result.category).isEqualTo(foodBag.category)
        assertThat(result.unitPrice).isEqualTo(foodBag.unitPrice)
        assertThat(result.allergens).isEqualTo(foodBag.allergens)
        assertThat(result.status).isEqualTo(OfferStatus.SCHEDULED)
        assertThat(result.totalQuantity).isEqualTo(totalQuantity)
        assertThat(result.availableQuantity).isEqualTo(totalQuantity)
        assertThat(result.reservedQuantity).isZero()
        assertThat(result.pickupWindow).isEqualTo(pickupWindow)
        assertThat(result.createdAt).isEqualTo(now)
        assertThat(result.updatedAt).isEqualTo(now)
        assertThat(result.version).isZero()

        verify(offerDBPort).findById(offerId)
        verify(offerAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(foodBag.id)
        verify(clock).instant()
        verify(offerDBPort).save(result)
        verify(eventFactory)
            .offerCreated(
                offer = result,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            offerAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenExistingOfferIsUpdated_returnsSavedOffer() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val existing =
            createOffer(
                storeId = storeId,
                availableQuantity = 3,
            )
        val foodBag =
            createFoodBag(
                id = existing.foodBagId,
                storeId = storeId,
            )
        val totalQuantity = 7
        val pickupWindow =
            PickupWindow(
                start = Instant.parse("2026-08-20T13:00:00Z"),
                end = Instant.parse("2026-08-20T15:00:00Z"),
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedOffer =
            createOffer(
                id = existing.id,
                storeId = storeId,
                foodBagId = existing.foodBagId,
                totalQuantity = totalQuantity,
                availableQuantity = 5,
                pickupWindow = pickupWindow,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .offerUpdated(
                    offer = savedOffer,
                    occurredAt = now,
                )

        `when`(offerDBPort.findById(existing.id)).thenReturn(existing)
        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)
        `when`(clock.instant()).thenReturn(now)
        `when`(offerDBPort.save(existing)).thenReturn(savedOffer)
        `when`(
                eventFactory.offerUpdated(
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
                offerId = existing.id,
                foodBagId = foodBag.id,
                totalQuantity = totalQuantity,
                pickupWindow = pickupWindow,
                requestedVersion = existing.version,
            )

        // Assert
        assertThat(result).isSameAs(savedOffer)
        assertThat(existing.totalQuantity).isEqualTo(totalQuantity)
        assertThat(existing.availableQuantity).isEqualTo(5)
        assertThat(existing.reservedQuantity).isEqualTo(2)
        assertThat(existing.pickupWindow).isEqualTo(pickupWindow)
        assertThat(existing.updatedAt).isEqualTo(now)

        verify(offerDBPort).findById(existing.id)
        verify(offerAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(foodBag.id)
        verify(clock).instant()
        verify(offerDBPort).save(existing)
        verify(eventFactory)
            .offerUpdated(
                offer = savedOffer,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            offerAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenExistingOfferBelongsToAnotherStore_throwsOfferNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val existing = createOffer()

        `when`(offerDBPort.findById(existing.id)).thenReturn(existing)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = existing.id,
                    foodBagId = existing.foodBagId,
                    totalQuantity = 7,
                    pickupWindow = existing.pickupWindow,
                    requestedVersion = existing.version,
                )
            }

        // Assert
        assertThat(exception.message).contains(existing.id.value.toString())

        verify(offerDBPort).findById(existing.id)
        verifyNoInteractions(
            foodBagDBPort,
            offerAccessPolicy,
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
        val offerId = OfferId(UUID.randomUUID())
        val foodBagId = FoodBagId(UUID.randomUUID())
        val expectedException = AccessDeniedException()

        `when`(offerDBPort.findById(offerId)).thenReturn(null)
        doThrow(expectedException)
            .`when`(offerAccessPolicy)
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
                    offerId = offerId,
                    foodBagId = foodBagId,
                    totalQuantity = 7,
                    pickupWindow =
                        PickupWindow(
                            start = Instant.parse("2026-08-20T12:00:00Z"),
                            end = Instant.parse("2026-08-20T14:00:00Z"),
                        ),
                    requestedVersion = 0,
                )
            }

        // Assert
        assertThat(exception).isSameAs(expectedException)

        verify(offerDBPort).findById(offerId)
        verify(offerAccessPolicy)
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
            offerAccessPolicy,
        )
    }

    @Test
    fun whenFoodBagDoesNotExist_throwsFoodBagNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val foodBagId = FoodBagId(UUID.randomUUID())

        `when`(offerDBPort.findById(offerId)).thenReturn(null)
        `when`(foodBagDBPort.findById(foodBagId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<FoodBagNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offerId,
                    foodBagId = foodBagId,
                    totalQuantity = 7,
                    pickupWindow =
                        PickupWindow(
                            start = Instant.parse("2026-08-20T12:00:00Z"),
                            end = Instant.parse("2026-08-20T14:00:00Z"),
                        ),
                    requestedVersion = 0,
                )
            }

        // Assert
        assertThat(exception.message).contains(foodBagId.value.toString())

        verify(offerDBPort).findById(offerId)
        verify(offerAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(foodBagId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            offerAccessPolicy,
        )
    }

    @Test
    fun whenFoodBagBelongsToAnotherStore_throwsFoodBagNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val foodBag = createFoodBag()

        `when`(offerDBPort.findById(offerId)).thenReturn(null)
        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)

        // Act
        val exception =
            assertThrows<FoodBagNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offerId,
                    foodBagId = foodBag.id,
                    totalQuantity = 7,
                    pickupWindow =
                        PickupWindow(
                            start = Instant.parse("2026-08-20T12:00:00Z"),
                            end = Instant.parse("2026-08-20T14:00:00Z"),
                        ),
                    requestedVersion = 0,
                )
            }

        // Assert
        assertThat(exception.message).contains(foodBag.id.value.toString())

        verify(offerDBPort).findById(offerId)
        verify(offerAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(foodBag.id)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            offerAccessPolicy,
        )
    }

    @Test
    fun whenFoodBagIsInactive_throwsInvalidStateException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val foodBag =
            createFoodBag(
                storeId = storeId,
                status = FoodBagStatus.INACTIVE,
            )

        `when`(offerDBPort.findById(offerId)).thenReturn(null)
        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offerId,
                    foodBagId = foodBag.id,
                    totalQuantity = 7,
                    pickupWindow =
                        PickupWindow(
                            start = Instant.parse("2026-08-20T12:00:00Z"),
                            end = Instant.parse("2026-08-20T14:00:00Z"),
                        ),
                    requestedVersion = 0,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("FoodBag must be ACTIVE to create or update an Offer")

        verify(offerDBPort).findById(offerId)
        verify(offerAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(foodBag.id)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            offerAccessPolicy,
        )
    }

    @Test
    fun whenNewOfferHasNonZeroVersion_throwsEntityVersionConflictException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())
        val foodBag = createFoodBag(storeId = storeId)
        val requestedVersion = 1L

        `when`(offerDBPort.findById(offerId)).thenReturn(null)
        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)

        // Act
        val exception =
            assertThrows<EntityVersionConflictException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offerId,
                    foodBagId = foodBag.id,
                    totalQuantity = 7,
                    pickupWindow =
                        PickupWindow(
                            start = Instant.parse("2026-08-20T12:00:00Z"),
                            end = Instant.parse("2026-08-20T14:00:00Z"),
                        ),
                    requestedVersion = requestedVersion,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo(
                "Version conflict for Offer '${offerId.value}': " +
                    "supplied version $requestedVersion does not match current version 0"
            )

        verify(offerDBPort).findById(offerId)
        verify(offerAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(foodBag.id)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            offerAccessPolicy,
        )
    }

    @Test
    fun whenUpdatedOfferUsesDifferentFoodBag_throwsOfferNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val existing = createOffer(storeId = storeId)
        val requestedFoodBag = createFoodBag(storeId = storeId)

        `when`(offerDBPort.findById(existing.id)).thenReturn(existing)
        `when`(foodBagDBPort.findById(requestedFoodBag.id)).thenReturn(requestedFoodBag)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = existing.id,
                    foodBagId = requestedFoodBag.id,
                    totalQuantity = 7,
                    pickupWindow = existing.pickupWindow,
                    requestedVersion = existing.version,
                )
            }

        // Assert
        assertThat(exception.message).contains(existing.id.value.toString())

        verify(offerDBPort).findById(existing.id)
        verify(offerAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(requestedFoodBag.id)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            offerAccessPolicy,
        )
    }

    @Test
    fun whenUpdatedOfferVersionDoesNotMatchCurrentVersion_throwsEntityVersionConflictException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val existing =
            createOffer(
                storeId = storeId,
                version = 2,
            )
        val foodBag =
            createFoodBag(
                id = existing.foodBagId,
                storeId = storeId,
            )
        val requestedVersion = 1L

        `when`(offerDBPort.findById(existing.id)).thenReturn(existing)
        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)

        // Act
        val exception =
            assertThrows<EntityVersionConflictException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = existing.id,
                    foodBagId = foodBag.id,
                    totalQuantity = 7,
                    pickupWindow = existing.pickupWindow,
                    requestedVersion = requestedVersion,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo(
                "Version conflict for Offer '${existing.id.value}': " +
                    "supplied version $requestedVersion does not match current version ${existing.version}"
            )

        verify(offerDBPort).findById(existing.id)
        verify(offerAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(foodBagDBPort).findById(foodBag.id)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            offerDBPort,
            foodBagDBPort,
            offerAccessPolicy,
        )
    }

    private fun createOffer(
        id: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        foodBagId: FoodBagId = FoodBagId(UUID.randomUUID()),
        status: OfferStatus = OfferStatus.SCHEDULED,
        totalQuantity: Int = 5,
        availableQuantity: Int = 5,
        pickupWindow: PickupWindow =
            PickupWindow(
                start = Instant.parse("2026-08-20T12:00:00Z"),
                end = Instant.parse("2026-08-20T14:00:00Z"),
            ),
        createdAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
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
            pickupWindow = pickupWindow,
            createdAt = createdAt,
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

    private fun anyOffer(): Offer = any(Offer::class.java) ?: createOffer()

    private fun anyInstant(): Instant = any(Instant::class.java) ?: Instant.EPOCH
}
