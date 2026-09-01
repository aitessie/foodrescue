package com.example.foodrescue.offerservice.application.access

import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.PartnerStoreNotFoundException
import com.example.foodrescue.offerservice.application.ports.CurrentUserPort
import com.example.foodrescue.offerservice.application.ports.PartnerStoreAccessPort
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PartnerStoreAccessSnapshot
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus
import com.example.foodrescue.offerservice.domain.`enum`.StoreStatus
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
class OfferAccessPolicyTest {
    @Mock private lateinit var currentUserPort: CurrentUserPort

    @Mock private lateinit var partnerStoreAccessPort: PartnerStoreAccessPort

    @InjectMocks private lateinit var accessPolicy: OfferAccessPolicy

    @Test
    fun whenAdminManagesStore_accessIsGranted() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val snapshot = createAccessSnapshot()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(snapshot)
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
        val snapshot = createAccessSnapshot(userIsManager = true)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(snapshot)
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
        val snapshot = createAccessSnapshot(userIsStaff = true)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(snapshot)
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
    fun whenUnassignedManagerIsAlsoAssignedStaff_accessIsGranted() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val snapshot = createAccessSnapshot(userIsStaff = true)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(snapshot)
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
    fun whenStoreDoesNotBelongToPartner_throwsPartnerStoreNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val snapshot = createAccessSnapshot(storeBelongsToPartner = false)

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(snapshot)

        // Act
        val exception =
            assertThrows<PartnerStoreNotFoundException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception).isExactlyInstanceOf(PartnerStoreNotFoundException::class.java)

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
        val snapshot =
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
            .thenReturn(snapshot)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Partner '$partnerId' is not active")

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
        val snapshot =
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
            .thenReturn(snapshot)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                accessPolicy.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("Store '$storeId' is not active")

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
        val snapshot = createAccessSnapshot()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(snapshot)
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
        assertThat(exception).isExactlyInstanceOf(AccessDeniedException::class.java)

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
        val snapshot = createAccessSnapshot()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(snapshot)
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
        assertThat(exception).isExactlyInstanceOf(AccessDeniedException::class.java)

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
    fun whenUserHasNoManagementRole_throwsAccessDeniedException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val snapshot = createAccessSnapshot()

        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)
        `when`(
                partnerStoreAccessPort.checkAccess(
                    partnerId = partnerId,
                    storeId = storeId,
                    userId = CURRENT_USER_ID,
                )
            )
            .thenReturn(snapshot)
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
        assertThat(exception).isExactlyInstanceOf(AccessDeniedException::class.java)

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
    fun whenOfferBelongsToRequestedStoreAndId_accessIsGranted() {
        // Arrange
        val offer = createOffer()

        // Act
        accessPolicy.checkOwnership(
            offer = offer,
            storeId = offer.storeId,
            offerId = offer.id,
        )

        // Assert
        verifyNoInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenOfferIdDoesNotMatch_throwsOfferNotFoundException() {
        // Arrange
        val offer = createOffer()
        val requestedOfferId = OfferId(UUID.randomUUID())

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                accessPolicy.checkOwnership(
                    offer = offer,
                    storeId = offer.storeId,
                    offerId = requestedOfferId,
                )
            }

        // Assert
        assertThat(exception.message).contains(requestedOfferId.value.toString())

        verifyNoInteractions(
            currentUserPort,
            partnerStoreAccessPort,
        )
    }

    @Test
    fun whenOfferBelongsToAnotherStore_throwsOfferNotFoundException() {
        // Arrange
        val offer = createOffer()
        val requestedStoreId = StoreId(UUID.randomUUID())

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                accessPolicy.checkOwnership(
                    offer = offer,
                    storeId = requestedStoreId,
                    offerId = offer.id,
                )
            }

        // Assert
        assertThat(exception.message).contains(offer.id.value.toString())

        verifyNoInteractions(
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
                    start = Instant.parse("2026-08-20T12:00:00Z"),
                    end = Instant.parse("2026-08-20T14:00:00Z"),
                ),
            createdAt = Instant.parse("2026-08-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-08-20T10:00:00Z"),
            version = 0,
        )

    companion object {
        private const val CURRENT_USER_ID = "33333333-3333-3333-3333-333333333333"
    }
}
