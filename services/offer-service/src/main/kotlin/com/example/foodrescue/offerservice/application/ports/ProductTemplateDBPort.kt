package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entity.ProductTemplate
import com.example.foodrescue.offerservice.domain.entity.ProductTemplateId

interface ProductTemplateDBPort {
    fun findById(id: ProductTemplateId): ProductTemplate?

    fun save(productTemplate: ProductTemplate): ProductTemplate
}
