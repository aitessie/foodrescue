package com.example.foodrescue.partnerservice.application.ports

import com.example.foodrescue.partnerservice.application.events.ApplicationEvent
import com.example.foodrescue.partnerservice.application.events.ApplicationEventPayload

interface DomainEventPublisherPort {
    fun publish(event: ApplicationEvent<ApplicationEventPayload>)
}
