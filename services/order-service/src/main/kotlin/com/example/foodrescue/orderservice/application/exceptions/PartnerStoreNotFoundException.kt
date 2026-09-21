package com.example.foodrescue.orderservice.application.exceptions

import com.example.foodrescue.orderservice.domain.entities.StoreId

class PartnerStoreNotFoundException(storeId: StoreId) :
    RuntimeException("Store not found in Partner Service: ${storeId.value}")
