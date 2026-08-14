package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.StoreAccessPolicy
import com.example.foodrescue.partnerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.partnerservice.application.exceptions.PartnerNotFoundException
import com.example.foodrescue.partnerservice.application.exceptions.StoreAccessDeniedException
import com.example.foodrescue.partnerservice.application.exceptions.StoreNotFoundException
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.domain.entities.Address
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import com.example.foodrescue.partnerservice.domain.entities.WorkingHours
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class CreateOrUpdateStoreUseCaseTest {

    private val storeDBPort = mock(StoreDBPort::class.java)
    private val partnerDBPort = mock(PartnerDBPort::class.java)
    private val storeAccessPolicy = mock(StoreAccessPolicy::class.java)

    private val updatedAt = Instant.parse("2026-01-02T10:00:00Z")

    private val clock =
        Clock.fixed(
            updatedAt,
            ZoneOffset.UTC,
        )

    private val createOrUpdateStoreUseCase =
        CreateOrUpdateStoreUseCase(
            storeDBPort = storeDBPort,
            partnerDBPort = partnerDBPort,
            storeAccessPolicy = storeAccessPolicy,
            clock = clock,
        )

    private val storeId = StoreId(UUID.randomUUID())

    private val partnerId = PartnerId(UUID.randomUUID())

    @Test
    fun whenStoreDoesNotExistCreatesStore() {
        // arrange
        val source = createStore()
        val partner = createPartner()

        // act
        `when`(storeDBPort.findById(storeId)).thenReturn(null)

        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)

        `when`(storeDBPort.save(source)).thenReturn(source)

        val result = createOrUpdateStoreUseCase.createOrUpdateStore(source)

        // assert
        assertThat(source).isEqualTo(result)

        verify(storeDBPort).findById(storeId)
        verify(partnerDBPort).findById(partnerId)

        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = source,
            )

        verify(storeDBPort).save(source)
    }

    @Test
    fun whenPartnerDoesNotExistThrowsPartnerNotFoundException() {
        // arrange
        val source = createStore()

        // act
        `when`(storeDBPort.findById(storeId)).thenReturn(null)

        `when`(partnerDBPort.findById(partnerId)).thenReturn(null)

        // assert
        assertThrows<PartnerNotFoundException> {
            createOrUpdateStoreUseCase.createOrUpdateStore(source)
        }

        verify(storeDBPort).findById(storeId)
        verify(partnerDBPort).findById(partnerId)
        verifyNoInteractions(storeAccessPolicy)
        verify(storeDBPort, never()).save(source)
    }

    @Test
    fun whenVersionsMatchUpdatesExistingStore() {
        // arrange
        val newAddress = mock(Address::class.java)
        val newWorkingHours = listOf(mock(WorkingHours::class.java))

        val existingStore =
            createStore(
                name = "Old name",
                version = 3,
            )

        val source =
            createStore(
                name = "Updated name",
                status = StoreStatus.entries.last(),
                workingHours = newWorkingHours,
                address = newAddress,
                version = 3,
            )

        // act
        `when`(storeDBPort.findById(storeId)).thenReturn(existingStore)

        `when`(storeDBPort.save(existingStore)).thenReturn(existingStore)

        val result = createOrUpdateStoreUseCase.createOrUpdateStore(source)

        // assert
        assertThat(existingStore).isEqualTo(result)
        assertThat("Updated name").isEqualTo(result.name)
        assertThat(source.status).isEqualTo(result.status)
        assertThat(newWorkingHours).isEqualTo(result.workingHours)
        assertThat(newAddress).isEqualTo(result.address)
        assertThat(updatedAt).isEqualTo(result.updatedAt)
        assertThat(3).isEqualTo(result.version)

        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingStore,
            )

        verify(storeDBPort).save(existingStore)
    }

    @Test
    fun whenStoreBelongsToAnotherPartnerThrowsStoreNotFoundException() {
        // arrange
        val anotherPartnerId = PartnerId(UUID.randomUUID())

        val existingStore =
            createStore(
                partnerId = anotherPartnerId,
                version = 1,
            )

        val source =
            createStore(
                partnerId = partnerId,
                version = 1,
            )

        // act
        `when`(storeDBPort.findById(storeId)).thenReturn(existingStore)

        // assert
        assertThrows<StoreNotFoundException> {
            createOrUpdateStoreUseCase.createOrUpdateStore(source)
        }

        verify(storeDBPort).findById(storeId)
        verifyNoInteractions(storeAccessPolicy)
        verify(storeDBPort, never()).save(existingStore)
    }

    @Test
    fun whenVersionsDifferThrowsEntityVersionConflictException() {
        // arrange
        val existingStore =
            createStore(
                name = "Existing name",
                version = 5,
            )

        val source =
            createStore(
                name = "Updated name",
                version = 4,
            )

        // act
        `when`(storeDBPort.findById(storeId)).thenReturn(existingStore)

        val exception =
            assertThrows<EntityVersionConflictException> {
                createOrUpdateStoreUseCase.createOrUpdateStore(source)
            }

        // assert
        assertThat(Store::class.simpleName).isEqualTo(exception.entityType)
        assertThat(storeId.value.toString()).isEqualTo(exception.entityId)
        assertThat(4).isEqualTo(exception.expectedVersion)
        assertThat(5).isEqualTo(exception.actualVersion)
        assertThat("Existing name").isEqualTo(existingStore.name)

        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingStore,
            )

        verify(storeDBPort, never()).save(existingStore)
    }

    @Test
    fun whenAccessIsDeniedDoeNotUpdateStore() {
        // arrange
        val existingStore =
            createStore(
                name = "Existing name",
                version = 2,
            )

        val source =
            createStore(
                name = "Updated name",
                version = 2,
            )

        // act
        `when`(storeDBPort.findById(storeId)).thenReturn(existingStore)

        doThrow(StoreAccessDeniedException())
            .`when`(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingStore,
            )

        // assert
        assertThrows<StoreAccessDeniedException> {
            createOrUpdateStoreUseCase.createOrUpdateStore(source)
        }

        assertEquals("Existing name", existingStore.name)

        verify(storeDBPort, never()).save(existingStore)
    }

    private fun createStore(
        id: StoreId = storeId,
        partnerId: PartnerId = this.partnerId,
        name: String = "Test store",
        status: StoreStatus = StoreStatus.entries.first(),
        workingHours: List<WorkingHours> = emptyList(),
        address: Address = mock(Address::class.java),
        version: Long = 0,
    ): Store {
        val createdAt = Instant.parse("2026-01-01T10:00:00Z")

        return Store(
            id = id,
            partnerId = partnerId,
            name = name,
            status = status,
            workingHours = workingHours,
            address = address,
            createdAt = createdAt,
            updatedAt = createdAt,
            version = version,
        )
    }

    private fun createPartner(
        id: PartnerId = partnerId,
        managerId: String = "manager-1",
    ): Partner {
        val createdAt = Instant.parse("2026-01-01T10:00:00Z")

        return Partner(
            id = id,
            managerId = managerId,
            name = "Test partner",
            status = PartnerStatus.entries.first(),
            createdAt = createdAt,
            updatedAt = createdAt,
            version = 0,
        )
    }
}
