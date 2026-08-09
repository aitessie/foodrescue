package com.example.foodrescue.partnerservice.adapter.input.web.system

import java.time.Instant
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/system")
class SystemController {

    @GetMapping("/secure-ping")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    fun securePing(authentication: Authentication): Map<String, Any> {
        return mapOf(
            "status" to "ok",
            "service" to "partner-service",
            "user" to authentication.name,
            "timestamp" to Instant.now().toString(),
        )
    }
}
