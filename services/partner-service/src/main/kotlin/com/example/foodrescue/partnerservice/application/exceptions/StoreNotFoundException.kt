package com.example.foodrescue.partnerservice.application.exceptions

import com.example.foodrescue.partnerservice.domain.entities.StoreId

class StoreNotFoundException(storeId: StoreId) :
    RuntimeException("Store with id $storeId was not found")
