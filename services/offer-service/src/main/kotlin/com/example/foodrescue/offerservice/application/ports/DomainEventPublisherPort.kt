package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entity.DomainEvent

interface DomainEventPublisherPort {
    fun publish(event: DomainEvent)
}
