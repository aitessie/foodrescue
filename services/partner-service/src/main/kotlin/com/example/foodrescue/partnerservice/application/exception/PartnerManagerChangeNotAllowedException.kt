package com.example.foodrescue.partnerservice.application.exception

import com.example.foodrescue.partnerservice.domain.entity.PartnerId

class PartnerManagerChangeNotAllowedException(val partnerId: PartnerId) :
    RuntimeException("Partner ${partnerId.value} manager cannot be changed")
