package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.exceptions.PartnerNotFoundException
import com.example.foodrescue.partnerservice.application.exceptions.StoreNotFoundException
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreStaffDBPort
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
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
    fun whenManagerChecksStoreAccessByPartnerAndStore_returnsManagerAccessSnapshot() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val partner = mock(Partner::class.java)
        val store = mock(Store::class.java)

        `when`(partner.status).thenReturn(PartnerStatus.ACTIVE)
        `when`(partner.managerId).thenReturn(CURRENT_USER_ID)
        `when`(store.partnerId).thenReturn(partnerId)
        `when`(store.status).thenReturn(StoreStatus.ACTIVE)
        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)
        `when`(storeDBPort.findById(storeId)).thenReturn(store)
        `when`(
            storeStaffDBPort.isStaffAssignedToStore(
                userId = CURRENT_USER_ID,
                storeId = storeId,
            )
        )
            .thenReturn(false)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )

        // Assert
        assertThat(result.partnerStatus).isEqualTo(PartnerStatus.ACTIVE)
        assertThat(result.storeStatus).isEqualTo(StoreStatus.ACTIVE)
        assertThat(result.userIsStoreManager).isTrue()
        assertThat(result.userIsStoreStaff).isFalse()

        verify(partnerDBPort).findById(partnerId)
        verify(storeDBPort).findById(storeId)
        verify(storeStaffDBPort)
            .isStaffAssignedToStore(
                userId = CURRENT_USER_ID,
                storeId = storeId,
            )
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
            storeStaffDBPort,
        )
    }

    @Test
    fun whenStaffChecksStoreAccessByPartnerAndStore_returnsStaffAccessSnapshot() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val partner = mock(Partner::class.java)
        val store = mock(Store::class.java)

        `when`(partner.status).thenReturn(PartnerStatus.ACTIVE)
        `when`(partner.managerId).thenReturn(OTHER_USER_ID)
        `when`(store.partnerId).thenReturn(partnerId)
        `when`(store.status).thenReturn(StoreStatus.ACTIVE)
        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)
        `when`(storeDBPort.findById(storeId)).thenReturn(store)
        `when`(
            storeStaffDBPort.isStaffAssignedToStore(
                userId = CURRENT_USER_ID,
                storeId = storeId,
            )
        )
            .thenReturn(true)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )

        // Assert
        assertThat(result.partnerStatus).isEqualTo(PartnerStatus.ACTIVE)
        assertThat(result.storeStatus).isEqualTo(StoreStatus.ACTIVE)
        assertThat(result.userIsStoreManager).isFalse()
        assertThat(result.userIsStoreStaff).isTrue()

        verify(partnerDBPort).findById(partnerId)
        verify(storeDBPort).findById(storeId)
        verify(storeStaffDBPort)
            .isStaffAssignedToStore(
                userId = CURRENT_USER_ID,
                storeId = storeId,
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
                    userId = CURRENT_USER_ID,
                )
            }

        // Assert
        assertThat(exception.message).contains(partnerId.value.toString())

        verify(partnerDBPort).findById(partnerId)
        verifyNoInteractions(
            storeDBPort,
            storeStaffDBPort,
        )
        verifyNoMoreInteractions(partnerDBPort)
    }

    @Test
    fun whenStoreDoesNotExistForPartnerAndStoreCheck_throwsStoreNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val partner = mock(Partner::class.java)

        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)
        `when`(storeDBPort.findById(storeId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<StoreNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            }

        // Assert
        assertThat(exception.message).contains(storeId.value.toString())

        verify(partnerDBPort).findById(partnerId)
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
        val partnerId = PartnerId(UUID.randomUUID())
        val actualPartnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val partner = mock(Partner::class.java)
        val store = mock(Store::class.java)

        `when`(store.partnerId).thenReturn(actualPartnerId)
        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)
        `when`(storeDBPort.findById(storeId)).thenReturn(store)

        // Act
        val exception =
            assertThrows<StoreNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            }

        // Assert
        assertThat(exception.message).contains(storeId.value.toString())

        verify(partnerDBPort).findById(partnerId)
        verify(storeDBPort).findById(storeId)
        verifyNoInteractions(storeStaffDBPort)
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
        )
    }

    @Test
    fun whenManagerChecksStoreAccessByStore_returnsManagerAccessSnapshot() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val partner = mock(Partner::class.java)
        val store = mock(Store::class.java)

        `when`(store.partnerId).thenReturn(partnerId)
        `when`(store.status).thenReturn(StoreStatus.SUSPENDED)
        `when`(partner.status).thenReturn(PartnerStatus.SUSPENDED)
        `when`(partner.managerId).thenReturn(CURRENT_USER_ID)
        `when`(storeDBPort.findById(storeId)).thenReturn(store)
        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)
        `when`(
            storeStaffDBPort.isStaffAssignedToStore(
                userId = CURRENT_USER_ID,
                storeId = storeId,
            )
        )
            .thenReturn(false)

        // Act
        val result =
            useCase.execute(
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )

        // Assert
        assertThat(result.partnerStatus).isEqualTo(PartnerStatus.SUSPENDED)
        assertThat(result.storeStatus).isEqualTo(StoreStatus.SUSPENDED)
        assertThat(result.userIsStoreManager).isTrue()
        assertThat(result.userIsStoreStaff).isFalse()

        verify(storeDBPort).findById(storeId)
        verify(partnerDBPort).findById(partnerId)
        verify(storeStaffDBPort)
            .isStaffAssignedToStore(
                userId = CURRENT_USER_ID,
                storeId = storeId,
            )
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
            storeStaffDBPort,
        )
    }

    @Test
    fun whenStaffChecksStoreAccessByStore_returnsStaffAccessSnapshot() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val partner = mock(Partner::class.java)
        val store = mock(Store::class.java)

        `when`(store.partnerId).thenReturn(partnerId)
        `when`(store.status).thenReturn(StoreStatus.ACTIVE)
        `when`(partner.status).thenReturn(PartnerStatus.ACTIVE)
        `when`(partner.managerId).thenReturn(OTHER_USER_ID)
        `when`(storeDBPort.findById(storeId)).thenReturn(store)
        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)
        `when`(
            storeStaffDBPort.isStaffAssignedToStore(
                userId = CURRENT_USER_ID,
                storeId = storeId,
            )
        )
            .thenReturn(true)

        // Act
        val result =
            useCase.execute(
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )

        // Assert
        assertThat(result.partnerStatus).isEqualTo(PartnerStatus.ACTIVE)
        assertThat(result.storeStatus).isEqualTo(StoreStatus.ACTIVE)
        assertThat(result.userIsStoreManager).isFalse()
        assertThat(result.userIsStoreStaff).isTrue()

        verify(storeDBPort).findById(storeId)
        verify(partnerDBPort).findById(partnerId)
        verify(storeStaffDBPort)
            .isStaffAssignedToStore(
                userId = CURRENT_USER_ID,
                storeId = storeId,
            )
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
            storeStaffDBPort,
        )
    }

    @Test
    fun whenStoreDoesNotExistForStoreCheck_throwsStoreNotFoundException() {
        // Arrange
        val storeId = StoreId(UUID.randomUUID())

        `when`(storeDBPort.findById(storeId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<StoreNotFoundException> {
                useCase.execute(
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            }

        // Assert
        assertThat(exception.message).contains(storeId.value.toString())

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
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val store = mock(Store::class.java)

        `when`(store.partnerId).thenReturn(partnerId)
        `when`(storeDBPort.findById(storeId)).thenReturn(store)
        `when`(partnerDBPort.findById(partnerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<PartnerNotFoundException> {
                useCase.execute(
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            }

        // Assert
        assertThat(exception.message).contains(partnerId.value.toString())

        verify(storeDBPort).findById(storeId)
        verify(partnerDBPort).findById(partnerId)
        verifyNoInteractions(storeStaffDBPort)
        verifyNoMoreInteractions(
            partnerDBPort,
            storeDBPort,
        )
    }

    companion object {
        private const val CURRENT_USER_ID = "33333333-3333-3333-3333-333333333333"

        private const val OTHER_USER_ID = "88888888-8888-8888-8888-888888888888"
    }
}
