package com.eventhub.api.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuración de Kafka.
 * Solo activa cuando app.kafka.enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Value("${app.kafka.topic.events}")
    private String eventsTopic;

    // 3 particiones para permitir paralelismo, 1 réplica porque es demo (en producción serían 3)
    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name(eventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
