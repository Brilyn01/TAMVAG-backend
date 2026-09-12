package com.tamvagbackend.service;

import com.tamvagbackend.dto.TransactionEventDtos.IngestionAckResponse;
import com.tamvagbackend.dto.TransactionEventDtos.TransactionIngestionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final LedgerService ledgerService;

    @Value("${tamva.kafka.topics.transaction-received:tamva.transaction.received}")
    private String transactionReceivedTopic;

    public TransactionEventProducer(KafkaTemplate<String, Object> kafkaTemplate, LedgerService ledgerService) {
        this.kafkaTemplate = kafkaTemplate;
        this.ledgerService = ledgerService;
    }

    public IngestionAckResponse publishTransactionEvent(TransactionIngestionEvent event) {
        String eventId = event.eventId() != null ? event.eventId() : "EVT_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String messageKey = event.accountId().toString() + ":" + event.sourceEventId();

        try {
            log.info("Publishing async transaction event {} to Kafka topic {}", eventId, transactionReceivedTopic);
            kafkaTemplate.send(transactionReceivedTopic, messageKey, event);

            return new IngestionAckResponse(
                    eventId,
                    "QUEUED_IN_KAFKA",
                    "Transaction event published to Kafka for async processing",
                    Instant.now()
            );
        } catch (Exception e) {
            log.warn("Kafka broker unreachable or not configured. Falling back to direct synchronous ingestion: {}", e.getMessage());
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
                    "PROCESSED_SYNCHRONOUSLY_FALLBACK",
                    "Kafka broker unavailable, transaction processed directly into ledger",
                    Instant.now()
            );
        }
    }
}
