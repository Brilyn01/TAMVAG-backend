package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.WebhookDelivery;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

        List<WebhookDelivery> findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
        List<String> statuses,
                Instant now
        );

        List<WebhookDelivery> findByWebhook_WebhookIdOrderByCreatedAtDesc(
                UUID webhookId
        );

        boolean existsByWebhook_WebhookIdAndEventId(
        UUID webhookId,
        UUID eventId
        );

        @Transactional
        @Modifying
        @Query("""
                UPDATE WebhookDelivery d
                SET d.status = 'DELIVERING',
                        d.attemptCount = d.attemptCount + 1,
                        d.lastAttemptAt = :now
                WHERE d.deliveryId = :deliveryId
                AND d.status = 'PENDING'
                AND d.nextAttemptAt <= :now
                """)
        int claimForDelivery(
                @Param("deliveryId") UUID deliveryId,
                @Param("now") Instant now
        );

        @Transactional
        @Modifying
        @Query("""
                UPDATE WebhookDelivery d
                SET d.status = 'PENDING',
                d.nextAttemptAt = :now
                WHERE d.status = 'DELIVERING'
                AND d.lastAttemptAt <= :staleBefore
                """)
        int requeueStaleDeliveries(
                @Param("now") Instant now,
                @Param("staleBefore") Instant staleBefore
        );
}