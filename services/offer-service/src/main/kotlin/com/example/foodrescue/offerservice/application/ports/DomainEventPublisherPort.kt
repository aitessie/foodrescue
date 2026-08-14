package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.application.events.ApplicationEvent
import com.example.foodrescue.offerservice.application.events.ApplicationEventPayload

interface DomainEventPublisherPort {
    fun publish(event: ApplicationEvent<ApplicationEventPayload>)
}
