package com.example.foodrescue.offerservice.configuration

import java.time.Duration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
class PartnerKafkaConsumerConfiguration {
    @Bean("partnerEventConsumerFactory")
    fun partnerEventConsumerFactory(
        kafkaProperties: KafkaProperties
    ): ConsumerFactory<String, String> =
        DefaultKafkaConsumerFactory<String, String>(kafkaProperties.buildConsumerProperties())

    @Bean
    fun partnerEventDeadLetterPublishingRecoverer(
        kafkaTemplate: KafkaTemplate<String, String>
    ): DeadLetterPublishingRecoverer =
        DeadLetterPublishingRecoverer(kafkaTemplate).apply {
            setFailIfSendResultIsError(true)
            setWaitForSendResultTimeout(Duration.ofSeconds(5))
        }

    @Bean
    fun partnerEventErrorHandler(recoverer: DeadLetterPublishingRecoverer): DefaultErrorHandler =
        DefaultErrorHandler(
                recoverer,
                FixedBackOff(
                    RETRY_INTERVAL_MILLISECONDS,
                    RETRY_ATTEMPTS,
                ),
            )
            .apply {
                setAckAfterHandle(true)
                setResetStateOnRecoveryFailure(true)
            }

    @Bean("partnerEventKafkaListenerContainerFactory")
    @Suppress("UsePropertyAccessSyntax")
    fun partnerEventKafkaListenerContainerFactory(
        @Qualifier("partnerEventConsumerFactory") consumerFactory: ConsumerFactory<String, String>,
        partnerEventErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(partnerEventErrorHandler)
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        }

    companion object {
        private const val RETRY_INTERVAL_MILLISECONDS = 250L
        private const val RETRY_ATTEMPTS = 2L
    }
}
