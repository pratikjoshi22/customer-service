package com.example.customerservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {
    // All consumer configuration is driven by application.yml
    // Spring Boot auto-configures ConsumerFactory and KafkaListenerContainerFactory
}
