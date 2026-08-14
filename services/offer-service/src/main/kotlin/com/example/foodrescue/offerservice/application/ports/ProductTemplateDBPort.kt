package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entities.ProductTemplate
import com.example.foodrescue.offerservice.domain.entities.ProductTemplateId

interface ProductTemplateDBPort {
    fun findById(id: ProductTemplateId): ProductTemplate?

    fun save(productTemplate: ProductTemplate): ProductTemplate
}
