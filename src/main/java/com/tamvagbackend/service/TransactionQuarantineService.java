package com.tamvagbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Connection;
import com.tamvagbackend.domain.entity.TransactionQuarantine;
import com.tamvagbackend.domain.repository.TransactionQuarantineRepository;
import com.tamvagbackend.service.connector.ConnectorProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TransactionQuarantineService {

    private final TransactionQuarantineRepository repository;
    private final ObjectMapper objectMapper;

    public TransactionQuarantineService(
            TransactionQuarantineRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionQuarantine quarantine(
            Connection connection,
            ConnectorProvider.ProviderTransaction transaction,
            String reason
    ) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection is required");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Quarantine reason is required");
        }

        TransactionQuarantine quarantine = new TransactionQuarantine();

        quarantine.setConnection(connection);
        quarantine.setSourceEventId(
                transaction != null ? transaction.sourceEventId() : null
        );
        quarantine.setSourceSystem(
                transaction != null ? transaction.sourceSystem() : null
        );
        quarantine.setAccountRefToken(
                transaction != null ? transaction.accountRefToken() : null
        );
        quarantine.setReason(reason.trim());
        quarantine.setStatus("OPEN");
        quarantine.setRawPayload(serialize(transaction));
        quarantine.setCreatedAt(Instant.now());

        return repository.save(quarantine);
    }

    private String serialize(
            ConnectorProvider.ProviderTransaction transaction
    ) {
        if (transaction == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(transaction);
        } catch (JsonProcessingException ex) {
            return "{\"serialization_error\":\"Provider transaction could not be serialized\"}";
        }
    }
}