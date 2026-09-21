package com.example.foodrescue.orderservice.application.exceptions

class PickupAccessDeniedException : RuntimeException("Current user cannot confirm pickup for this Store")
