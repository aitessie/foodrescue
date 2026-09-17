package com.example.foodrescue.orderservice.application.ports

import com.example.foodrescue.orderservice.application.events.ApplicationEvent
import com.example.foodrescue.orderservice.application.events.ApplicationEventPayload

interface DomainEventPublisherPort {
    fun publish(event: ApplicationEvent<ApplicationEventPayload>)
}
