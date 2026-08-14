package com.example.foodrescue.offerservice.application.exceptions

import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId

class PartnerStoreNotFoundException(
    partnerId: PartnerId,
    storeId: StoreId,
) : NotFoundException("Partner/store hierarchy '$partnerId/$storeId' was not found")
