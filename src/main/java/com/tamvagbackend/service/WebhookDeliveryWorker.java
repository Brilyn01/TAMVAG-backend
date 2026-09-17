package com.tamvagbackend.service;

import com.tamvagbackend.config.WebhookProperties;
import com.tamvagbackend.domain.entity.WebhookDelivery;
import com.tamvagbackend.domain.repository.WebhookDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class WebhookDeliveryWorker {

    private static final Logger log =
            LoggerFactory.getLogger(WebhookDeliveryWorker.class);

    private static final int MAX_RESPONSE_BODY_LENGTH = 4000;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final RestClient.Builder restClientBuilder;
    private final WebhookSecretService webhookSecretService;
    private final WebhookSignatureService webhookSignatureService;
    private final WebhookProperties webhookProperties;

    public WebhookDeliveryWorker(
            WebhookDeliveryRepository webhookDeliveryRepository,
            RestClient.Builder restClientBuilder,
            WebhookSecretService webhookSecretService,
            WebhookSignatureService webhookSignatureService,
            WebhookProperties webhookProperties
    ) {
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.restClientBuilder = restClientBuilder;
        this.webhookSecretService = webhookSecretService;
        this.webhookSignatureService = webhookSignatureService;
        this.webhookProperties = webhookProperties;
    }

    public void processDueDeliveries() {
        Instant now = Instant.now();

        Instant staleBefore =
                now.minusSeconds(
                        webhookProperties.getStaleDeliverySeconds()
                );

        int requeued =
                webhookDeliveryRepository.requeueStaleDeliveries(
                        now,
                        staleBefore
                );

        if (requeued > 0) {
            log.warn(
                    "Requeued {} stale webhook deliveries",
                    requeued
            );
        }

        List<WebhookDelivery> deliveries =
                webhookDeliveryRepository
                        .findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                                List.of("PENDING"),
                                now
                        );

        for (WebhookDelivery delivery : deliveries) {
            deliver(delivery);
        }
    }

    private void deliver(WebhookDelivery delivery) {

        Instant now = Instant.now();

        int claimed = webhookDeliveryRepository.claimForDelivery(
                delivery.getDeliveryId(),
                now
        );

        if (claimed == 0) {
            log.debug(
                    "Webhook delivery {} was already claimed by another worker",
                    delivery.getDeliveryId()
            );
            return;
        }

        delivery = webhookDeliveryRepository
                .findById(delivery.getDeliveryId())
                .orElse(null);

        if (delivery == null) {
            log.warn(
                    "Webhook delivery {} disappeared after being claimed",
                    delivery.getDeliveryId()
            );
            return;
        }

        int attempt = delivery.getAttemptCount();

        try {
            String secret = webhookSecretService.decrypt(
                    delivery.getWebhook().getSigningSecretCiphertext()
            );

            String signature =
                    webhookSignatureService.sign(
                            secret,
                            delivery.getPayload()
                    );

            ResponseEntity<String> response =
                    restClientBuilder
                            .requestFactory(requestFactory())
                            .build()
                            .post()
                            .uri(delivery.getWebhook().getUrl())
                            .header(
                                    "X-TAMVA-Event-Id",
                                    delivery.getEventId().toString()
                            )
                            .header(
                                    "X-TAMVA-Event-Type",
                                    delivery.getEventType()
                            )
                            .header(
                                    "X-TAMVA-Delivery-Id",
                                    delivery.getDeliveryId().toString()
                            )
                            .header(
                                    "X-TAMVA-Timestamp",
                                    String.valueOf(Instant.now().getEpochSecond())
                            )
                            .header(
                                    "X-TAMVA-Signature",
                                    "sha256=" + signature
                            )
                            .body(delivery.getPayload())
                            .exchange((request, clientResponse) -> {

                                String responseBody = null;

                                if (clientResponse.getBody() != null) {
                                    responseBody = new String(
                                            clientResponse.getBody().readAllBytes(),
                                            StandardCharsets.UTF_8
                                    );
                                }

                                return ResponseEntity
                                        .status(clientResponse.getStatusCode())
                                        .body(responseBody);
                            });

            int responseStatus = response.getStatusCode().value();

            delivery.setResponseStatus(responseStatus);
            delivery.setResponseBody(
                    truncateResponseBody(response.getBody())
            );

            if (response.getStatusCode().is2xxSuccessful()) {

                markDelivered(delivery);

                log.info(
                        "Webhook delivery {} succeeded with HTTP {}",
                        delivery.getDeliveryId(),
                        responseStatus
                );

            } else {

                String error =
                        "Webhook endpoint returned HTTP " + responseStatus;

                if (attempt >= webhookProperties.getMaxAttempts()) {

                    delivery.setLastError(
                            truncateError(error)
                    );

                    markExhausted(delivery);

                    log.error(
                            "Webhook delivery {} exhausted after {} attempts; HTTP {}",
                            delivery.getDeliveryId(),
                            attempt,
                            responseStatus
                    );

                } else {

                    scheduleRetry(
                            delivery,
                            error
                    );

                    log.warn(
                            "Webhook delivery {} returned HTTP {} on attempt {}",
                            delivery.getDeliveryId(),
                            responseStatus,
                            attempt
                    );
                }
            }

        } catch (Exception ex) {

            String error =
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : ex.getClass().getSimpleName();

            delivery.setLastError(
                    truncateError(error)
            );

            if (attempt >= webhookProperties.getMaxAttempts()) {

                markExhausted(delivery);

                log.error(
                        "Webhook delivery {} exhausted after {} attempts",
                        delivery.getDeliveryId(),
                        attempt,
                        ex
                );

            } else {

                scheduleRetry(
                        delivery,
                        error
                );

                log.warn(
                        "Webhook delivery {} failed on attempt {}: {}",
                        delivery.getDeliveryId(),
                        attempt,
                        delivery.getLastError()
                );
            }
        }

        webhookDeliveryRepository.save(delivery);
    }

    private void markDelivered(WebhookDelivery delivery) {

        delivery.setStatus("DELIVERED");
        delivery.setCompletedAt(Instant.now());
        delivery.setNextAttemptAt(Instant.now());
        delivery.setLastError(null);
    }

    private void markExhausted(WebhookDelivery delivery) {

        delivery.setStatus("EXHAUSTED");
        delivery.setCompletedAt(Instant.now());
    }

    private void scheduleRetry(
            WebhookDelivery delivery,
            String error
    ) {

        int attempt = delivery.getAttemptCount();

        long delaySeconds =
                calculateRetryDelay(attempt);

        delivery.setStatus("PENDING");

        delivery.setNextAttemptAt(
                Instant.now().plusSeconds(delaySeconds)
        );

        delivery.setLastError(
                truncateError(error)
        );
    }

    private long calculateRetryDelay(int attempt) {

        long initial =
                webhookProperties.getInitialRetrySeconds();

        long maximum =
                webhookProperties.getMaxRetrySeconds();

        int exponent = Math.max(0, attempt - 1);

        if (exponent >= 62) {
            return maximum;
        }

        long multiplier = 1L << exponent;

        long delay;

        try {
            delay = Math.multiplyExact(
                    initial,
                    multiplier
            );
        } catch (ArithmeticException ex) {
            delay = maximum;
        }

        return Math.min(
                delay,
                maximum
        );
    }

    private org.springframework.http.client.ClientHttpRequestFactory requestFactory() {

        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(
                Duration.ofSeconds(10)
        );

        factory.setReadTimeout(
                Duration.ofSeconds(30)
        );

        return factory;
    }

    private String truncateResponseBody(String body) {

        if (body == null) {
            return null;
        }

        if (body.length() <= MAX_RESPONSE_BODY_LENGTH) {
            return body;
        }

        return body.substring(
                0,
                MAX_RESPONSE_BODY_LENGTH
        );
    }

    private String truncateError(String error) {

        if (error == null) {
            return null;
        }

        if (error.length() <= MAX_ERROR_LENGTH) {
            return error;
        }

        return error.substring(
                0,
                MAX_ERROR_LENGTH
        );
    }
}