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
import java.nio.charset.StandardCharsets;

@Service
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);

    private final WebhookDeliveryService webhookDeliveryService;

    private final LedgerService ledgerService;
    private final Optional<org.springframework.kafka.core.KafkaTemplate<String, Object>> kafkaTemplate;

    @Value("${tamva.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${tamva.kafka.topics.transaction-received:tamva.transaction.received}")
    private String transactionReceivedTopic;

    public TransactionEventProducer(
        LedgerService ledgerService,
        Optional<org.springframework.kafka.core.KafkaTemplate<String, Object>> kafkaTemplate,
        WebhookDeliveryService webhookDeliveryService
    ) {
        this.ledgerService = ledgerService;
        this.kafkaTemplate = kafkaTemplate;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    public IngestionAckResponse publishTransactionEvent(TransactionIngestionEvent event) {
        String eventId = event.eventId() != null && !event.eventId().isBlank()
                ? event.eventId()
                : "EVT_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        String messageKey = event.accountId().toString() + ":" + event.sourceEventId();

        TransactionIngestionEvent eventWithId = new TransactionIngestionEvent(
                eventId,
                event.accountId(),
                event.customerId(),
                event.sourceEventId(),
                event.direction(),
                event.amount(),
                event.currency(),
                event.occurredAt(),
                event.channel(),
                event.counterparty(),
                event.reference(),
                event.sourceSystem()
        );

        if (kafkaEnabled && kafkaTemplate.isPresent()) {
            try {
                log.info(
                        "Publishing async transaction event {} to Kafka topic {}",
                        eventId,
                        transactionReceivedTopic
                );

                kafkaTemplate.get().send(
                        transactionReceivedTopic,
                        messageKey,
                        eventWithId
                );

                return new IngestionAckResponse(
                        eventId,
                        "QUEUED_IN_KAFKA",
                        "Transaction event published to Kafka for async processing",
                        Instant.now()
                );
            } catch (Exception e) {
                log.warn(
                        "Kafka broker unreachable. Falling back to synchronous ledger ingestion: {}",
                        e.getMessage()
                );
            }
        } else {
            log.info(
                    "Kafka disabled. Processing transaction event {} directly into ledger.",
                    eventId
            );
        }

        ledgerService.recordTransaction(
            eventWithId.accountId(),
            eventWithId.sourceEventId(),
            eventWithId.direction(),
            eventWithId.amount(),
            eventWithId.currency() != null ? eventWithId.currency() : "GHS",
            eventWithId.occurredAt(),
            eventWithId.channel(),
            eventWithId.counterparty(),
            eventWithId.reference(),
            eventWithId.sourceSystem()
        );

        UUID webhookEventId = toWebhookEventId(eventWithId);

        webhookDeliveryService.enqueueEvent(
                webhookEventId,
                "transaction.created",
                eventWithId
        );

        return new IngestionAckResponse(
                eventId,
                "PROCESSED_SYNCHRONOUSLY",
                "Transaction processed directly into ledger (Kafka not in use)",
                Instant.now()
        );
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