package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.StoreAccessPolicy
import com.example.foodrescue.partnerservice.application.exceptions.StoreAccessDeniedException
import com.example.foodrescue.partnerservice.application.exceptions.StoreNotFoundException
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.domain.entities.Address
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class GetStoreUseCaseTest {
    private val storeDBPort = mock(StoreDBPort::class.java)
    private val storeAccessPolicy = mock(StoreAccessPolicy::class.java)
    private val getStoreUseCase =
        GetStoreUseCase(
            storeDBPort = storeDBPort,
            storeAccessPolicy = storeAccessPolicy,
        )

    private val storeId = StoreId(UUID.randomUUID())
    private val partnerId = PartnerId(UUID.randomUUID())

    @Test
    fun whenStoreExistsAndAccessAllowedReturnsStore() {
        // arrange
        val store = createStore()

        // act
        `when`(storeDBPort.findById(storeId)).thenReturn(store)

        val result =
            getStoreUseCase.getStore(
                partnerId = partnerId,
                storeId = storeId,
            )

        // assert
        assertThat(store).isEqualTo(result)

        verify(storeDBPort).findById(storeId)
        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.READ,
                resource = store,
            )
    }

    @Test
    fun whenStoreDoesNotExistDoesNotCheckAccessAndThrowsStoreNotFoundException() {
        // act
        `when`(storeDBPort.findById(storeId)).thenReturn(null)

        // assert
        assertThrows<StoreNotFoundException> {
            getStoreUseCase.getStore(
                partnerId = partnerId,
                storeId = storeId,
            )
        }

        verify(storeDBPort).findById(storeId)
        verifyNoInteractions(storeAccessPolicy)
    }

    @Test
    fun whenStoreBelongsToAnotherPartnerThrowsStoreNotFoundException() {
        // arrange
        val anotherPartnerId = PartnerId(UUID.randomUUID())

        val store = createStore(partnerId = anotherPartnerId)

        // act
        `when`(storeDBPort.findById(storeId)).thenReturn(store)

        // assert
        assertThrows<StoreNotFoundException> {
            getStoreUseCase.getStore(
                partnerId = partnerId,
                storeId = storeId,
            )
        }
        verify(storeDBPort).findById(storeId)
        verifyNoInteractions(storeAccessPolicy)
    }

    @Test
    fun whenAccessDeniedThrowsStoreAccessDeniedException() {
        // arrange
        val store = createStore()

        `when`(storeDBPort.findById(storeId)).thenReturn(store)

        // act
        doThrow(StoreAccessDeniedException())
            .`when`(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.READ,
                resource = store,
            )
        // assert
        assertThrows<StoreAccessDeniedException> {
            getStoreUseCase.getStore(
                partnerId = partnerId,
                storeId = storeId,
            )
        }

        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.READ,
                resource = store,
            )
    }

    private fun createStore(
        id: StoreId = storeId,
        partnerId: PartnerId = this.partnerId,
        name: String = "Test store",
        status: StoreStatus = StoreStatus.entries.first(),
        version: Long = 0,
    ): Store {
        val createdAt = Instant.parse("2026-01-01T10:00:00Z")

        return Store(
            id = id,
            partnerId = partnerId,
            name = name,
            status = status,
            workingHours = emptyList(),
            address = mock(Address::class.java),
            createdAt = createdAt,
            updatedAt = createdAt,
            version = version,
        )
    }
}
