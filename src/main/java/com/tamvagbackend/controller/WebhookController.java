package com.tamvagbackend.controller;

import com.tamvagbackend.dto.WebhookDtos.CreateWebhookRequest;
import com.tamvagbackend.dto.WebhookDtos.CreateWebhookResponse;
import com.tamvagbackend.dto.WebhookDtos.WebhookDeliveryResponse;
import com.tamvagbackend.dto.WebhookDtos.WebhookResponse;
import com.tamvagbackend.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/webhooks")
@Tag(
        name = "Webhooks",
        description = "Partner webhook subscriptions and event delivery"
)
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(
            WebhookService webhookService
    ) {
        this.webhookService = webhookService;
    }

    @PostMapping
    @Operation(
            summary = "Create webhook subscription",
            description =
                    "Creates an active webhook subscription and returns "
                            + "its signing secret once"
    )
    public ResponseEntity<CreateWebhookResponse> createWebhook(
            @Valid @RequestBody CreateWebhookRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(webhookService.createWebhook(request));
    }

    @GetMapping
    @Operation(
            summary = "List application webhooks",
            description =
                    "Returns webhook subscriptions belonging to an application"
    )
    public ResponseEntity<List<WebhookResponse>> getWebhooks(
            @RequestParam("application_id") UUID applicationId
    ) {

        return ResponseEntity.ok(
                webhookService.getWebhooks(applicationId)
        );
    }

    @GetMapping("/{webhookId}/deliveries")
    @Operation(
            summary = "List webhook deliveries",
            description =
                    "Returns delivery history for a webhook subscription"
    )
    public ResponseEntity<List<WebhookDeliveryResponse>> getDeliveries(
            @PathVariable UUID webhookId
    ) {

        return ResponseEntity.ok(
                webhookService.getDeliveries(webhookId)
        );
    }

    @DeleteMapping("/{webhookId}")
    @Operation(
            summary = "Deactivate webhook",
            description =
                    "Deactivates a webhook subscription"
    )
    public ResponseEntity<Void> deactivateWebhook(
            @PathVariable UUID webhookId
    ) {

        webhookService.deactivateWebhook(webhookId);

        return ResponseEntity.noContent().build();
    }
}