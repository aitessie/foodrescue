package com.example.foodrescue.offerservice.adapter.`out`.db.persistence

import com.example.foodrescue.offerservice.adapter.`out`.db.entities.OfferReservationJpaEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface OfferReservationJpaRepository : JpaRepository<OfferReservationJpaEntity, UUID>
