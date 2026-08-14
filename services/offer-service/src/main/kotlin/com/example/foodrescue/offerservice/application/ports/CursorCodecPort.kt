package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entities.NormalizedOfferSearchFilter
import com.example.foodrescue.offerservice.domain.entities.OfferCursorData

interface CursorCodecPort {
    fun decode(cursor: String): OfferCursorData

    fun encode(cursor: OfferCursorData): String

    fun fingerprint(filter: NormalizedOfferSearchFilter): String
}
