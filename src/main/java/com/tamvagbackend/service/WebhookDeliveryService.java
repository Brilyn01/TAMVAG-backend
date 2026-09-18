package com.tamvagbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.WebhookDelivery;
import com.tamvagbackend.domain.entity.WebhookSubscription;
import com.tamvagbackend.domain.repository.WebhookDeliveryRepository;
import com.tamvagbackend.domain.repository.WebhookSubscriptionRepository;
import com.tamvagbackend.dto.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private static final Logger log =
            LoggerFactory.getLogger(WebhookDeliveryService.class);

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;

    public WebhookDeliveryService(
            WebhookSubscriptionRepository subscriptionRepository,
            WebhookDeliveryRepository deliveryRepository,
            ObjectMapper objectMapper
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int enqueueEvent(
            UUID eventId,
            String eventType,
            Object data
    ) {

        if (eventId == null) {
            throw new IllegalArgumentException("eventId is required");
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }

        WebhookEvent event = new WebhookEvent(
                eventId,
                eventType,
                Instant.now(),
                data
        );

        String payload = serialize(event);

        List<WebhookSubscription> subscriptions =
                subscriptionRepository.findByStatus("ACTIVE");

        int created = 0;

        for (WebhookSubscription webhook : subscriptions) {

            if (!subscribedTo(webhook, eventType)) {
                continue;
            }

            /*
             * The unique constraint on (webhook_id, event_id) is the
             * database-level idempotency guard.
             */
            if (deliveryRepository.existsByWebhook_WebhookIdAndEventId(
                webhook.getWebhookId(),
                eventId
            )) {
                continue;
            }

            try {
                WebhookDelivery delivery = new WebhookDelivery();

                delivery.setWebhook(webhook);
                delivery.setEventId(eventId);
                delivery.setEventType(eventType);
                delivery.setPayload(payload);
                delivery.setStatus("PENDING");
                delivery.setAttemptCount(0);
                delivery.setNextAttemptAt(Instant.now());

                deliveryRepository.save(delivery);

                created++;

            } catch (DataIntegrityViolationException ex) {
                /*
                * Another worker may have created the same delivery between
                * the existence check and the insert. The database unique
                * constraint makes that operation idempotent.
                */
                log.debug(
                        "Webhook delivery already exists for webhook {} and event {}",
                        webhook.getWebhookId(),
                        eventId
                );
            }
        }

        return created;
    }

    private boolean subscribedTo(
            WebhookSubscription webhook,
            String eventType
    ) {

        try {
            List<String> events = objectMapper.readValue(
                    webhook.getEvents(),
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class)
            );

            return events.contains("*") || events.contains(eventType);

        } catch (Exception ex) {

            log.error(
                    "Invalid event configuration for webhook {}",
                    webhook.getWebhookId(),
                    ex
            );

            return false;
        }
    }

    private String serialize(WebhookEvent event) {

        try {
            return objectMapper.writeValueAsString(event);

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to serialize webhook event",
                    ex
            );
        }
    }
}