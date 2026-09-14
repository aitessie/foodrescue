package com.example.foodrescue.orderservice.application.exceptions

class OrderAccessDeniedException : RuntimeException("Current user has no access to this Order")
