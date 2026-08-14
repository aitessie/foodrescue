package com.example.foodrescue.partnerservice.application.exceptions

import com.example.foodrescue.partnerservice.domain.entities.PartnerId

class PartnerManagerChangeNotAllowedException(val partnerId: PartnerId) :
    RuntimeException("Partner ${partnerId.value} manager cannot be changed")
