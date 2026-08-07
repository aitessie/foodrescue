package com.example.foodrescue.partnerservice.configuration

import org.modelmapper.ModelMapper
import org.modelmapper.convention.MatchingStrategies
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ModelMapperConfig {
    @Bean
    fun modelMapper(): ModelMapper =
        ModelMapper().apply {
            configuration.matchingStrategy = MatchingStrategies.STRICT
        }
}
