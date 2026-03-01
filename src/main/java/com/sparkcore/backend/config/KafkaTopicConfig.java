package com.sparkcore.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    /**
     * Stellt sicher, dass das Topic "transaction-events" beim Start
     * der Spring Boot Anwendung in Kafka erstellt wird, falls es nicht
     * bereits existiert.
     */
    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name("transaction-events")
                // Für dieses Projekt reicht 1 Partition und 1 Replica völlig aus
                .partitions(1)
                .replicas(1)
                .build();
    }
}
