package com.tamvagbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Application;
import com.tamvagbackend.domain.entity.WebhookDelivery;
import com.tamvagbackend.domain.entity.WebhookSubscription;
import com.tamvagbackend.domain.repository.ApplicationRepository;
import com.tamvagbackend.domain.repository.WebhookDeliveryRepository;
import com.tamvagbackend.domain.repository.WebhookSubscriptionRepository;
import com.tamvagbackend.dto.WebhookDtos.CreateWebhookRequest;
import com.tamvagbackend.dto.WebhookDtos.CreateWebhookResponse;
import com.tamvagbackend.dto.WebhookDtos.WebhookDeliveryResponse;
import com.tamvagbackend.dto.WebhookDtos.WebhookResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class WebhookService {

    private static final int SECRET_BYTES = 32;

    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ApplicationRepository applicationRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final WebhookSecretService webhookSecretService;

    private final SecureRandom secureRandom = new SecureRandom();

    public WebhookService(
            WebhookSubscriptionRepository webhookSubscriptionRepository,
            WebhookDeliveryRepository webhookDeliveryRepository,
            ApplicationRepository applicationRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            WebhookSecretService webhookSecretService
    ) {
        this.webhookSubscriptionRepository = webhookSubscriptionRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.applicationRepository = applicationRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.webhookSecretService = webhookSecretService;
    }

    @Transactional
    public CreateWebhookResponse createWebhook(CreateWebhookRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Webhook request is required");
        }

        if (request.applicationId() == null) {
            throw new IllegalArgumentException("application_id is required");
        }

        if (request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("url is required");
        }

        if (request.events() == null || request.events().isEmpty()) {
            throw new IllegalArgumentException(
                    "events must contain at least one event"
            );
        }

        List<String> events = request.events().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        if (events.isEmpty()) {
            throw new IllegalArgumentException(
                    "events must contain at least one valid event"
            );
        }

        Application application = applicationRepository
                .findById(request.applicationId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Application not found")
                );

        String signingSecret = generateSigningSecret();

        WebhookSubscription webhook = new WebhookSubscription();

        webhook.setApplication(application);
        webhook.setUrl(request.url().trim());
        webhook.setStatus("ACTIVE");
        webhook.setEvents(writeEvents(events));
        webhook.setSigningSecretCiphertext(
                webhookSecretService.encrypt(signingSecret)
        );

        WebhookSubscription saved =
                webhookSubscriptionRepository.save(webhook);

        auditService.logEvent(
                "PARTNER",
                application.getApplicationId().toString(),
                "WEBHOOK_CREATED",
                "WEBHOOK",
                saved.getWebhookId().toString(),
                null,
                safeAuditPayload(saved)
        );

        return new CreateWebhookResponse(
                toResponse(saved),
                signingSecret
        );
    }

    @Transactional(readOnly = true)
    public List<WebhookResponse> getWebhooks(UUID applicationId) {

        if (applicationId == null) {
            throw new IllegalArgumentException(
                    "application_id is required"
            );
        }

        return webhookSubscriptionRepository
                .findByApplication_ApplicationId(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryResponse> getDeliveries(
            UUID webhookId
    ) {

        if (webhookId == null) {
            throw new IllegalArgumentException(
                    "webhook_id is required"
            );
        }

        if (!webhookSubscriptionRepository.existsById(webhookId)) {
            throw new IllegalArgumentException(
                    "Webhook not found"
            );
        }

        return webhookDeliveryRepository
                .findByWebhook_WebhookIdOrderByCreatedAtDesc(webhookId)
                .stream()
                .map(this::toDeliveryResponse)
                .toList();
    }

    @Transactional
    public void deactivateWebhook(UUID webhookId) {

        if (webhookId == null) {
            throw new IllegalArgumentException(
                    "webhook_id is required"
            );
        }

        WebhookSubscription webhook =
                webhookSubscriptionRepository.findById(webhookId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Webhook not found"
                                )
                        );

        if ("INACTIVE".equals(webhook.getStatus())) {
            return;
        }

        webhook.setStatus("INACTIVE");

        webhookSubscriptionRepository.save(webhook);

        auditService.logEvent(
                "PARTNER",
                webhook.getApplication()
                        .getApplicationId()
                        .toString(),
                "WEBHOOK_DEACTIVATED",
                "WEBHOOK",
                webhook.getWebhookId().toString(),
                null,
                null
        );
    }

    private WebhookResponse toResponse(
            WebhookSubscription webhook
    ) {

        List<String> events = readEvents(webhook.getEvents());

        return new WebhookResponse(
                webhook.getWebhookId(),
                webhook.getApplication().getApplicationId(),
                webhook.getUrl(),
                webhook.getStatus(),
                events,
                webhook.getCreatedAt(),
                webhook.getUpdatedAt()
        );
    }

    private WebhookDeliveryResponse toDeliveryResponse(
            WebhookDelivery delivery
    ) {

        return new WebhookDeliveryResponse(
                delivery.getDeliveryId(),
                delivery.getWebhook().getWebhookId(),
                delivery.getEventId(),
                delivery.getEventType(),
                delivery.getStatus(),
                delivery.getAttemptCount(),
                delivery.getNextAttemptAt(),
                delivery.getLastAttemptAt(),
                delivery.getResponseStatus(),
                delivery.getLastError(),
                delivery.getCreatedAt(),
                delivery.getCompletedAt()
        );
    }

    private String generateSigningSecret() {

        byte[] secret = new byte[SECRET_BYTES];

        secureRandom.nextBytes(secret);

        return "whsec_"
                + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(secret);
    }

    private String writeEvents(List<String> events) {

        try {
            return objectMapper.writeValueAsString(events);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Unable to serialize webhook events",
                    ex
            );
        }
    }

    private List<String> readEvents(String events) {

        try {
            return objectMapper.readValue(
                    events,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(
                                    List.class,
                                    String.class
                            )
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Invalid webhook event configuration",
                    ex
            );
        }
    }

    private String safeAuditPayload(
            WebhookSubscription webhook
    ) {

        try {
            return objectMapper.writeValueAsString(
                    new AuditWebhookPayload(
                            webhook.getUrl(),
                            readEvents(webhook.getEvents())
                    )
            );
        } catch (JsonProcessingException ex) {
            return "{\"url\":\"redacted\"}";
        }
    }

    private record AuditWebhookPayload(
            String url,
            List<String> events
    ) {}
}