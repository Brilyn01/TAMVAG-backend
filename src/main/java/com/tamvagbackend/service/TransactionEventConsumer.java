package com.tamvagbackend.service;

import com.tamvagbackend.dto.TransactionEventDtos.TransactionIngestionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final LedgerService ledgerService;
    private final RedisIdempotencyService idempotencyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${tamva.kafka.topics.transaction-normalised:tamva.transaction.normalised}")
    private String normalisedTopic;

    @Value("${tamva.kafka.topics.transaction-dlq:tamva.transaction.dlq}")
    private String dlqTopic;

    public TransactionEventConsumer(
            LedgerService ledgerService,
            RedisIdempotencyService idempotencyService,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "${tamva.kafka.topics.transaction-received:tamva.transaction.received}",
            groupId = "${spring.kafka.consumer.group-id:tamva-ingestion-group}"
    )
    public void consumeTransactionEvent(TransactionIngestionEvent event) {
        log.info("Kafka Consumer received transaction event from account {} with sourceEventId {}", event.accountId(), event.sourceEventId());

        String idempotencyLockKey = event.accountId().toString() + ":" + event.sourceEventId();

        // 1. Redis Distributed Idempotency Lock
        boolean acquired = idempotencyService.acquireIdempotencyLock(idempotencyLockKey, 3600); // 1 hour TTL
        if (!acquired) {
            log.warn("Redis Idempotency Check: Transaction event already processed or currently locked: {}", idempotencyLockKey);
            return;
        }

        try {
            // 2. Normalize and record into PostgreSQL double-entry ledger
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

            log.info("Successfully ingested transaction event {} into ledger", event.sourceEventId());

            // 3. Publish to normalised topic for risk engine & features processing
            kafkaTemplate.send(normalisedTopic, idempotencyLockKey, event);

        } catch (Exception e) {
            log.error("Failed to process transaction event {}, routing to DLQ {}: {}", event.sourceEventId(), dlqTopic, e.getMessage(), e);
            idempotencyService.releaseLock(idempotencyLockKey);

            try {
                kafkaTemplate.send(dlqTopic, idempotencyLockKey, event);
            } catch (Exception dlqException) {
                log.error("Failed to publish to DLQ topic: {}", dlqException.getMessage());
            }
        }
    }
}
