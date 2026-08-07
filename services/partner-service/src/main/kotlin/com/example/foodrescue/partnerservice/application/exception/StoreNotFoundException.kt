package com.example.foodrescue.partnerservice.application.exception

import com.example.foodrescue.partnerservice.domain.entity.StoreId

class StoreNotFoundException(storeId: StoreId) :
    RuntimeException("Store with id $storeId was not found")
