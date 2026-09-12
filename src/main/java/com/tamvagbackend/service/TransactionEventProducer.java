package com.tamvagbackend.service;

import com.tamvagbackend.dto.TransactionEventDtos.IngestionAckResponse;
import com.tamvagbackend.dto.TransactionEventDtos.TransactionIngestionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Publishes transaction events to Kafka when enabled, or falls back to direct
 * synchronous ledger ingestion when Kafka is disabled (tamva.kafka.enabled=false).
 */
@Service
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);

    private final LedgerService ledgerService;

    // KafkaTemplate is optional — only injected when spring-kafka auto-configures it
    private final Optional<org.springframework.kafka.core.KafkaTemplate<String, Object>> kafkaTemplate;

    @Value("${tamva.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${tamva.kafka.topics.transaction-received:tamva.transaction.received}")
    private String transactionReceivedTopic;

    public TransactionEventProducer(
            LedgerService ledgerService,
            Optional<org.springframework.kafka.core.KafkaTemplate<String, Object>> kafkaTemplate
    ) {
        this.ledgerService = ledgerService;
        this.kafkaTemplate = kafkaTemplate;
    }

    public IngestionAckResponse publishTransactionEvent(TransactionIngestionEvent event) {
        String eventId = event.eventId() != null
                ? event.eventId()
                : "EVT_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String messageKey = event.accountId().toString() + ":" + event.sourceEventId();

        if (kafkaEnabled && kafkaTemplate.isPresent()) {
            try {
                log.info("Publishing async transaction event {} to Kafka topic {}", eventId, transactionReceivedTopic);
                kafkaTemplate.get().send(transactionReceivedTopic, messageKey, event);
                return new IngestionAckResponse(
                        eventId,
                        "QUEUED_IN_KAFKA",
                        "Transaction event published to Kafka for async processing",
                        Instant.now()
                );
            } catch (Exception e) {
                log.warn("Kafka broker unreachable. Falling back to synchronous ledger ingestion: {}", e.getMessage());
            }
        } else {
            log.info("Kafka disabled. Processing transaction event {} directly into ledger.", eventId);
        }

        // Synchronous path — works perfectly without any broker
        ledgerService.recordTransaction(
                event.accountId(),
                event.sourceEventId(),
                event.direction(),
                event.amount(),
                event.currency() != null ? event.currency() : "GHS",
                event.occurredAt(),
                event.channel(),
                event.counterparty(),
                event.reference(),
                event.sourceSystem()
        );

        return new IngestionAckResponse(
                eventId,
                "PROCESSED_SYNCHRONOUSLY",
                "Transaction processed directly into ledger (Kafka not in use)",
                Instant.now()
        );
    }
}
