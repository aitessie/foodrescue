package com.example.foodrescue.offerservice.application.access

import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.NotFoundException
import com.example.foodrescue.offerservice.application.ports.CurrentUserPort
import com.example.foodrescue.offerservice.application.ports.PartnerStoreAccessPort
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PartnerStoreAccessSnapshot
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus
import com.example.foodrescue.offerservice.domain.`enum`.StoreStatus
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
class FoodBagAccessPolicyTest {
    @Mock private lateinit var currentUserPort: CurrentUserPort

    @Mock private lateinit var partnerStoreAccessPort: PartnerStoreAccessPort

    @InjectMocks private lateinit var accessPolicy: FoodBagAccessPolicy

    @Test
    fun whenAdminManagesStore_accessIsGranted() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access = createAccessSnapshot()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(true)

        // Act
        accessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = storeId,
        )

        // Assert
        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenAssignedManagerManagesStore_accessIsGranted() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access = createAccessSnapshot(userIsManager = true)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(true)

        // Act
        accessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = storeId,
        )

        // Assert
        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenAssignedStaffManagesStore_accessIsGranted() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access = createAccessSnapshot(userIsStaff = true)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(true)

        // Act
        accessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = storeId,
        )

        // Assert
        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenManagerIsNotAssignedButAssignedStaffManagesStore_accessIsGranted() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access = createAccessSnapshot(userIsStaff = true)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(true)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(true)

        // Act
        accessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = storeId,
        )

        // Assert
        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenStoreDoesNotBelongToPartner_throwsNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access = createAccessSnapshot(storeBelongsToPartner = false)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)

        // Act
        val exception =
            assertThrows<NotFoundException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Store ${storeId.value} was not found for Partner ${partnerId.value}")

        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenPartnerIsNotActive_throwsInvalidStateException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access =
            createAccessSnapshot(
                partnerStatus = PartnerStatus.entries.first { it != PartnerStatus.ACTIVE }
            )

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Partner ${partnerId.value} must be ACTIVE")

        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenStoreIsNotActive_throwsInvalidStateException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access =
            createAccessSnapshot(
                storeStatus = StoreStatus.entries.first { it != StoreStatus.ACTIVE }
            )

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Store ${storeId.value} must be ACTIVE")

        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort, never()).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenManagerIsNotAssignedToStore_throwsAccessDeniedException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access = createAccessSnapshot()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(true)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(false)

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot manage Store ${storeId.value}")

        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenStaffIsNotAssignedToStore_throwsAccessDeniedException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access = createAccessSnapshot()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(true)

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot manage Store ${storeId.value}")

        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenCustomerRequestsAccess_throwsAccessDeniedException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access = createAccessSnapshot()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(false)

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot manage Store ${storeId.value}")

        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenUserHasNoApplicationRole_throwsAccessDeniedException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val access = createAccessSnapshot()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(access)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.MANAGER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.STAFF)).thenReturn(false)

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot manage Store ${storeId.value}")

        verify(currentUserPort).getUserId()
        verify(partnerStoreAccessPort)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
                userId = CURRENT_USER_ID,
            )
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(currentUserPort).hasRole(ApplicationRole.MANAGER)
        verify(currentUserPort).hasRole(ApplicationRole.STAFF)
        verifyNoMoreInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    private fun createAccessSnapshot(
        partnerStatus: PartnerStatus = PartnerStatus.ACTIVE,
        storeStatus: StoreStatus = StoreStatus.ACTIVE,
        storeBelongsToPartner: Boolean = true,
        userIsManager: Boolean = false,
        userIsStaff: Boolean = false,
    ): PartnerStoreAccessSnapshot =
        PartnerStoreAccessSnapshot(
            partnerStatus = partnerStatus,
            storeStatus = storeStatus,
            storeBelongsToPartner = storeBelongsToPartner,
            userIsManager = userIsManager,
            userIsStaff = userIsStaff,
        )

    companion object {
        private const val CURRENT_USER_ID = "33333333-3333-3333-3333-333333333333"
    }
}
