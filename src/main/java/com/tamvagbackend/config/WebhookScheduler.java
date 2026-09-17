package com.tamvagbackend.config;

import com.tamvagbackend.service.WebhookDeliveryWorker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebhookScheduler {

    private final WebhookDeliveryWorker deliveryWorker;

    public WebhookScheduler(
            WebhookDeliveryWorker deliveryWorker
    ) {
        this.deliveryWorker = deliveryWorker;
    }

    @Scheduled(fixedDelayString = "${tamva.webhook.poll-interval-ms:5000}")
    public void processWebhookDeliveries() {
        deliveryWorker.processDueDeliveries();
    }
}