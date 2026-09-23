package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.exceptions.PartnerNotFoundException
import com.example.foodrescue.partnerservice.application.exceptions.StoreNotFoundException
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreStaffDBPort
import com.example.foodrescue.partnerservice.domain.entities.Address
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
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
class CheckPartnerStoreAccessUseCaseTest {
    @Mock private lateinit var partnerDBPort: PartnerDBPort

    @Mock private lateinit var storeDBPort: StoreDBPort

    @Mock private lateinit var storeStaffDBPort: StoreStaffDBPort

    @InjectMocks private lateinit var useCase: CheckPartnerStoreAccessUseCase

    @Test
    fun whenPartnerAndStoreExistAndUserIsManager_returnsAccessSnapshot() {
        // Arrange
        val partner =
            createPartner(
                managerId = USER_ID,
                status = PartnerStatus.SUSPENDED,
            )
        val store =
            createStore(
                partnerId = partner.id,
                status = StoreStatus.ACTIVE,
            )

        `when`(partnerDBPort.findById(partner.id)).thenReturn(partner)
        `when`(storeDBPort.findById(store.id)).thenReturn(store)
        `when`(
                storeStaffDBPort.isStaffAssignedToStore(
                    userId = USER_ID,
                    storeId = store.id,
                )
            )
            .thenReturn(false)

        // Act
        val result =
            useCase.execute(
                partnerId = partner.id,
                storeId = store.id,
                userId = USER_ID,
            )

        // Assert
        assertThat(result.partnerStatus).isEqualTo(PartnerStatus.SUSPENDED)
        assertThat(result.storeStatus).isEqualTo(StoreStatus.ACTIVE)
        assertThat(result.userIsStoreManager).isTrue()
        assertThat(result.userIsStoreStaff).isFalse()

        verify(partnerDBPort).findById(partner.id)
        verify(storeDBPort).findById(store.id)
        verify(storeStaffDBPort)
            .isStaffAssignedToStore(
                userId = USER_ID,
                storeId = store.id,
            )
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
            storeStaffDBPort,
        )
    }

    @Test
    fun whenPartnerDoesNotExist_throwsPartnerNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())

        `when`(partnerDBPort.findById(partnerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<PartnerNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = USER_ID,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Partner with id ${partnerId.value} was not found")

        verify(partnerDBPort).findById(partnerId)
        verifyNoInteractions(
            storeDBPort,
            storeStaffDBPort,
        )
        verifyNoMoreInteractions(partnerDBPort)
    }

    @Test
    fun whenStoreDoesNotExistForExplicitPartner_throwsStoreNotFoundException() {
        // Arrange
        val partner = createPartner()
        val storeId = StoreId(UUID.randomUUID())

        `when`(partnerDBPort.findById(partner.id)).thenReturn(partner)
        `when`(storeDBPort.findById(storeId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<StoreNotFoundException> {
                useCase.execute(
                    partnerId = partner.id,
                    storeId = storeId,
                    userId = USER_ID,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Store with id ${storeId.value} was not found")

        verify(partnerDBPort).findById(partner.id)
        verify(storeDBPort).findById(storeId)
        verifyNoInteractions(storeStaffDBPort)
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
        )
    }

    @Test
    fun whenStoreBelongsToAnotherPartner_throwsStoreNotFoundException() {
        // Arrange
        val requestedPartner = createPartner()
        val actualPartnerId = PartnerId(UUID.randomUUID())
        val store = createStore(partnerId = actualPartnerId)

        `when`(partnerDBPort.findById(requestedPartner.id)).thenReturn(requestedPartner)
        `when`(storeDBPort.findById(store.id)).thenReturn(store)

        // Act
        val exception =
            assertThrows<StoreNotFoundException> {
                useCase.execute(
                    partnerId = requestedPartner.id,
                    storeId = store.id,
                    userId = USER_ID,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Store with id ${store.id.value} was not found")

        verify(partnerDBPort).findById(requestedPartner.id)
        verify(storeDBPort).findById(store.id)
        verifyNoInteractions(storeStaffDBPort)
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
        )
    }

    @Test
    fun whenStoreAndPartnerExistAndUserIsStaff_returnsAccessSnapshot() {
        // Arrange
        val partner =
            createPartner(
                managerId = OTHER_USER_ID,
                status = PartnerStatus.ACTIVE,
            )
        val store =
            createStore(
                partnerId = partner.id,
                status = StoreStatus.SUSPENDED,
            )

        `when`(storeDBPort.findById(store.id)).thenReturn(store)
        `when`(partnerDBPort.findById(partner.id)).thenReturn(partner)
        `when`(
                storeStaffDBPort.isStaffAssignedToStore(
                    userId = USER_ID,
                    storeId = store.id,
                )
            )
            .thenReturn(true)

        // Act
        val result =
            useCase.execute(
                storeId = store.id,
                userId = USER_ID,
            )

        // Assert
        assertThat(result.partnerStatus).isEqualTo(PartnerStatus.ACTIVE)
        assertThat(result.storeStatus).isEqualTo(StoreStatus.SUSPENDED)
        assertThat(result.userIsStoreManager).isFalse()
        assertThat(result.userIsStoreStaff).isTrue()

        verify(storeDBPort).findById(store.id)
        verify(partnerDBPort).findById(partner.id)
        verify(storeStaffDBPort)
            .isStaffAssignedToStore(
                userId = USER_ID,
                storeId = store.id,
            )
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
            storeStaffDBPort,
        )
    }

    @Test
    fun whenStoreDoesNotExistWithoutExplicitPartner_throwsStoreNotFoundException() {
        // Arrange
        val storeId = StoreId(UUID.randomUUID())

        `when`(storeDBPort.findById(storeId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<StoreNotFoundException> {
                useCase.execute(
                    storeId = storeId,
                    userId = USER_ID,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Store with id ${storeId.value} was not found")

        verify(storeDBPort).findById(storeId)
        verifyNoInteractions(
            partnerDBPort,
            storeStaffDBPort,
        )
        verifyNoMoreInteractions(storeDBPort)
    }

    @Test
    fun whenStorePartnerDoesNotExist_throwsPartnerNotFoundException() {
        // Arrange
        val store = createStore()

        `when`(storeDBPort.findById(store.id)).thenReturn(store)
        `when`(partnerDBPort.findById(store.partnerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<PartnerNotFoundException> {
                useCase.execute(
                    storeId = store.id,
                    userId = USER_ID,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Partner with id ${store.partnerId.value} was not found")

        verify(storeDBPort).findById(store.id)
        verify(partnerDBPort).findById(store.partnerId)
        verifyNoInteractions(storeStaffDBPort)
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
        )
    }

    private fun createPartner(
        id: PartnerId = PartnerId(UUID.randomUUID()),
        managerId: String = OTHER_USER_ID,
        name: String = "Partner",
        status: PartnerStatus = PartnerStatus.ACTIVE,
        createdAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
    ): Partner =
        Partner(
            id = id,
            managerId = managerId,
            name = name,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    private fun createStore(
        id: StoreId = StoreId(UUID.randomUUID()),
        partnerId: PartnerId = PartnerId(UUID.randomUUID()),
        name: String = "Store",
        status: StoreStatus = StoreStatus.ACTIVE,
        createdAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
    ): Store =
        Store(
            id = id,
            partnerId = partnerId,
            name = name,
            status = status,
            workingHours = emptyList(),
            address =
                Address(
                    city = "Saint Petersburg",
                    street = "Nevsky Prospekt",
                    building = "1",
                    postalCode = "190000",
                ),
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    companion object {
        private const val USER_ID = "33333333-3333-3333-3333-333333333333"
        private const val OTHER_USER_ID = "88888888-8888-8888-8888-888888888888"
    }
}
