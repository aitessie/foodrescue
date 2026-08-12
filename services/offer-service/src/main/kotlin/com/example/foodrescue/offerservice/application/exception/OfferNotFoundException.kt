package com.example.foodrescue.offerservice.application.exception

import com.example.foodrescue.offerservice.domain.entity.OfferId

class OfferNotFoundException(offerId: OfferId) : NotFoundException("Offer '$offerId' was not found")
