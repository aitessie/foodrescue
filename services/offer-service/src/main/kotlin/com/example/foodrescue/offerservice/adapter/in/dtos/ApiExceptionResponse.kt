package com.example.foodrescue.offerservice.adapter.`in`.dtos

import java.time.Instant

data class ApiExceptionResponse(
    val timestamp: Instant,
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
)
