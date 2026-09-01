package com.example.foodrescue.offerservice.adapter.`in`.mappers

import com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos.PartnerEventEnvelope
import com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos.PartnerEventPayloadV1
import com.example.foodrescue.offerservice.adapter.`in`.kafka.dtos.StoreEventPayloadV1
import com.example.foodrescue.offerservice.application.exceptions.InvalidPartnerEventException
import com.example.foodrescue.offerservice.application.exceptions.UnsupportedPartnerStatusException
import com.example.foodrescue.offerservice.application.exceptions.UnsupportedStoreStatusException
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PartnerStatusSnapshotUpdate
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.entities.StoreSnapshotUpdate
import com.example.foodrescue.offerservice.domain.`enum`.PartnerStatus
import com.example.foodrescue.offerservice.domain.`enum`.StoreStatus
import java.time.DateTimeException
import java.time.ZoneId
import org.springframework.stereotype.Component

@Component
class PartnerEventMapper {
    fun toStoreSnapshot(
        envelope: PartnerEventEnvelope,
        payload: StoreEventPayloadV1,
    ): StoreSnapshotUpdate =
        StoreSnapshotUpdate(
            storeId = StoreId(payload.storeId),
            partnerId = PartnerId(payload.partnerId),
            partnerStatus = resolvePartnerStatus(payload.partnerStatus),
            storeStatus = resolveStoreStatus(payload.storeStatus),
            name = payload.name,
            address = payload.address,
            timeZone = resolveTimeZone(payload.timeZone),
            storeVersion = envelope.aggregateVersion,
        )

    fun toPartnerStatusUpdate(
        envelope: PartnerEventEnvelope,
        payload: PartnerEventPayloadV1,
    ): PartnerStatusSnapshotUpdate =
        PartnerStatusSnapshotUpdate(
            partnerId = PartnerId(payload.partnerId),
            partnerStatus = resolvePartnerStatus(payload.partnerStatus),
            partnerVersion = envelope.aggregateVersion,
        )

    private fun resolvePartnerStatus(statusCode: String): PartnerStatus =
        PartnerStatus.entries.firstOrNull { status ->
            status.code == statusCode
        } ?: throw UnsupportedPartnerStatusException(statusCode)

    private fun resolveStoreStatus(statusCode: String): StoreStatus =
        StoreStatus.entries.firstOrNull { status ->
            status.code == statusCode
        } ?: throw UnsupportedStoreStatusException(statusCode)

    private fun resolveTimeZone(timeZone: String): ZoneId =
        try {
            ZoneId.of(timeZone)
        } catch (exception: DateTimeException) {
            throw InvalidPartnerEventException(
                message = "Partner event contains invalid store timeZone: $timeZone"
            )
        }
}
