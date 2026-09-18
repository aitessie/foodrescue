package com.example.foodrescue.orderservice.configuration

import java.time.Duration
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Suppress("UsePropertyAccessSyntax")
@Configuration
class OfferKafkaConsumerConfiguration {
    @Bean("offerEventKafkaListenerContainerFactory")
    fun offerEventKafkaListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        kafkaTemplate: KafkaTemplate<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val consumerFactory =
            DefaultKafkaConsumerFactory<String, String>(kafkaProperties.buildConsumerProperties())

        val recoverer =
            DeadLetterPublishingRecoverer(kafkaTemplate).apply {
                setFailIfSendResultIsError(true)
                setWaitForSendResultTimeout(Duration.ofSeconds(5))
            }

        val errorHandler =
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

        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(errorHandler)
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        }
    }

    companion object {
        private const val RETRY_INTERVAL_MILLISECONDS = 250L
        private const val RETRY_ATTEMPTS = 2L
    }
}
