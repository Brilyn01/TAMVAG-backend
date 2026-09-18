package com.tamvagbackend.service;

import com.tamvagbackend.dto.TransactionEventDtos.TransactionIngestionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "tamva.kafka.enabled", havingValue = "true")
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final LedgerService ledgerService;
    private final RedisIdempotencyService idempotencyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebhookDeliveryService webhookDeliveryService;

    @Value("${tamva.kafka.topics.transaction-normalised:tamva.transaction.normalised}")
    private String normalisedTopic;

    @Value("${tamva.kafka.topics.transaction-dlq:tamva.transaction.dlq}")
    private String dlqTopic;

    public TransactionEventConsumer(
            LedgerService ledgerService,
            RedisIdempotencyService idempotencyService,
            KafkaTemplate<String, Object> kafkaTemplate,
            WebhookDeliveryService webhookDeliveryService
    ) {
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
        this.kafkaTemplate = kafkaTemplate;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @KafkaListener(
            topics = "${tamva.kafka.topics.transaction-received:tamva.transaction.received}",
            groupId = "${spring.kafka.consumer.group-id:tamva-ingestion-group}"
    )
    public void consumeTransactionEvent(TransactionIngestionEvent event) {
        log.info(
                "Kafka Consumer received transaction event from account {} with sourceEventId {}",
                event.accountId(),
                event.sourceEventId()
        );

        String idempotencyLockKey =
                event.accountId().toString() + ":" + event.sourceEventId();

        boolean acquired =
                idempotencyService.acquireIdempotencyLock(idempotencyLockKey, 3600);

        if (!acquired) {
            log.warn(
                    "Redis Idempotency Check: Transaction event already processed or currently locked: {}",
                    idempotencyLockKey
            );
            return;
        }

        try {
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

            log.info(
                    "Successfully ingested transaction event {} into ledger",
                    event.sourceEventId()
            );

            /*
             * The webhook is emitted only after successful ledger ingestion.
             *
             * event_id is part of the public transaction event contract and is
             * converted into a deterministic UUID for webhook delivery
             * persistence. If an event_id is unavailable, the stable
             * account_id + source_event_id identity is used instead.
             */
            UUID webhookEventId = toWebhookEventId(event);

            webhookDeliveryService.enqueueEvent(
                    webhookEventId,
                    "transaction.created",
                    event
            );

            kafkaTemplate.send(
                    normalisedTopic,
                    idempotencyLockKey,
                    event
            );

        } catch (Exception e) {
            log.error(
                    "Failed to process transaction event {}, routing to DLQ {}: {}",
                    event.sourceEventId(),
                    dlqTopic,
                    e.getMessage(),
                    e
            );

            idempotencyService.releaseLock(idempotencyLockKey);

            try {
                kafkaTemplate.send(
                        dlqTopic,
                        idempotencyLockKey,
                        event
                );
            } catch (Exception dlqException) {
                log.error(
                        "Failed to publish to DLQ topic: {}",
                        dlqException.getMessage()
                );
            }
        }
    }

    private UUID toWebhookEventId(TransactionIngestionEvent event) {
        if (event.eventId() != null && !event.eventId().isBlank()) {
            try {
                return UUID.fromString(event.eventId());
            } catch (IllegalArgumentException ignored) {
                return UUID.nameUUIDFromBytes(
                        event.eventId().getBytes(StandardCharsets.UTF_8)
                );
            }
        }

        String stableIdentity =
                event.accountId().toString() + ":" + event.sourceEventId();

        return UUID.nameUUIDFromBytes(
                stableIdentity.getBytes(StandardCharsets.UTF_8)
        );
    }
}