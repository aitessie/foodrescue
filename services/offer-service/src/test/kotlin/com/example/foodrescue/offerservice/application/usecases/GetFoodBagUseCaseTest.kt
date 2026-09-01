package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.FoodBagAccessPolicy
import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.FoodBagNotFoundException
import com.example.foodrescue.offerservice.application.ports.FoodBagDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
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
class GetFoodBagUseCaseTest {
    @Mock private lateinit var foodBagDBPort: FoodBagDBPort

    @Mock private lateinit var accessPolicy: FoodBagAccessPolicy

    @InjectMocks private lateinit var useCase: GetFoodBagUseCase

    @Test
    fun whenUserRequestsExistingFoodBag_returnsFoodBag() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val foodBag = createFoodBag(storeId = storeId)

        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                foodBagId = foodBag.id,
            )

        // Assert
        assertThat(result).isSameAs(foodBag)

        verify(foodBagDBPort).findById(foodBag.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verifyNoMoreInteractions(
            foodBagDBPort,
            accessPolicy,
        )
    }

    @Test
    fun whenUserRequestsMissingFoodBag_throwsFoodBagNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val foodBagId = FoodBagId(UUID.randomUUID())

        `when`(foodBagDBPort.findById(foodBagId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<FoodBagNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    foodBagId = foodBagId,
                )
            }

        // Assert
        assertThat(exception.message).contains(foodBagId.value.toString())

        verify(foodBagDBPort).findById(foodBagId)
        verifyNoInteractions(accessPolicy)
        verifyNoMoreInteractions(foodBagDBPort)
    }

    @Test
    fun whenUserRequestsFoodBagFromAnotherStore_throwsFoodBagNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val foodBag = createFoodBag()

        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)

        // Act
        val exception =
            assertThrows<FoodBagNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    foodBagId = foodBag.id,
                )
            }

        // Assert
        assertThat(exception.message).contains(foodBag.id.value.toString())

        verify(foodBagDBPort).findById(foodBag.id)
        verifyNoInteractions(accessPolicy)
        verifyNoMoreInteractions(foodBagDBPort)
    }

    @Test
    fun whenUserCannotManageStore_throwsAccessDeniedException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val foodBag = createFoodBag(storeId = storeId)
        val expectedMessage = "Current user cannot manage Store ${storeId.value}"

        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)
        doThrow(AccessDeniedException(expectedMessage))
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
                    foodBagId = foodBag.id,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo(expectedMessage)

        verify(foodBagDBPort).findById(foodBag.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verifyNoMoreInteractions(
            foodBagDBPort,
            accessPolicy,
        )
    }

    private fun createFoodBag(
        id: FoodBagId = FoodBagId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
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
            status = FoodBagStatus.ACTIVE,
            createdAt = Instant.parse("2026-08-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-08-20T10:00:00Z"),
            version = 0,
        )
}
