package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookSubscriptionRepository
        extends JpaRepository<WebhookSubscription, UUID> {

    List<WebhookSubscription> findByApplication_ApplicationId(
            UUID applicationId
    );

    List<WebhookSubscription> findByStatus(String status);
}