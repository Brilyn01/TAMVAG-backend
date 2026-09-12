package com.tamvagbackend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${tamva.kafka.topics.transaction-received:tamva.transaction.received}")
    private String transactionReceivedTopic;

    @Value("${tamva.kafka.topics.transaction-normalised:tamva.transaction.normalised}")
    private String transactionNormalisedTopic;

    @Value("${tamva.kafka.topics.transaction-dlq:tamva.transaction.dlq}")
    private String transactionDlqTopic;

    @Bean
    public NewTopic transactionReceivedTopic() {
        log.info("Creating Kafka Topic: {}", transactionReceivedTopic);
        return TopicBuilder.name(transactionReceivedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionNormalisedTopic() {
        log.info("Creating Kafka Topic: {}", transactionNormalisedTopic);
        return TopicBuilder.name(transactionNormalisedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionDlqTopic() {
        log.info("Creating Kafka DLQ Topic: {}", transactionDlqTopic);
        return TopicBuilder.name(transactionDlqTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
