package com.example.foodrescue.offerservice.adapter.`out`.db.mappers

import com.example.foodrescue.offerservice.adapter.`out`.db.entities.OfferReservationJpaEntity
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import org.springframework.stereotype.Component

@Component
class OfferReservationJpaMapper {
    fun toDomain(entity: OfferReservationJpaEntity): OfferReservation =
        OfferReservation(
            id = ReservationId(entity.id),
            offerId = OfferId(entity.offerId),
            customerId = entity.customerId,
            quantity = entity.quantity,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            version = entity.version,
        )

    fun toJpaEntity(reservation: OfferReservation): OfferReservationJpaEntity =
        OfferReservationJpaEntity(
            id = reservation.id.value,
            offerId = reservation.offerId.value,
            customerId = reservation.customerId,
            quantity = reservation.quantity,
            status = reservation.status,
            createdAt = reservation.createdAt,
            updatedAt = reservation.updatedAt,
            version = reservation.version,
        )
}
