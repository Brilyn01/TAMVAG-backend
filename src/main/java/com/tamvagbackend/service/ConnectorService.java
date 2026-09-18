package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Connection;
import com.tamvagbackend.domain.repository.AccountRepository;
import com.tamvagbackend.domain.repository.ConnectionRepository;
import com.tamvagbackend.dto.AuditDtos.ConnectorSyncRequest;
import com.tamvagbackend.dto.AuditDtos.ConnectorSyncResponse;
import com.tamvagbackend.service.connector.ConnectorProvider;
import com.tamvagbackend.service.connector.ConnectorProviderRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ConnectorService {

    private final ConnectionRepository connectionRepository;
    private final AccountRepository accountRepository;
    private final ConsentAuthorizationService consentAuthorizationService;
    private final ConnectorProviderRegistry providerRegistry;
    private final LedgerService ledgerService;
    private final AuditService auditService;

    public ConnectorService(
            ConnectionRepository connectionRepository,
            AccountRepository accountRepository,
            ConsentAuthorizationService consentAuthorizationService,
            ConnectorProviderRegistry providerRegistry,
            LedgerService ledgerService,
            AuditService auditService
    ) {
        this.connectionRepository = connectionRepository;
        this.accountRepository = accountRepository;
        this.consentAuthorizationService = consentAuthorizationService;
        this.providerRegistry = providerRegistry;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
    }

    @Transactional
    public ConnectorSyncResponse sync(
            UUID connectionId,
            ConnectorSyncRequest request,
            UUID authenticatedInstitutionId
    ) {
        if (request.customerId() == null || request.institutionId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "customer_id and institution_id are required"
            );
        }

        if (!authenticatedInstitutionId.equals(request.institutionId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Institution does not match authenticated application"
            );
        }

        Connection connection = connectionRepository
                .findByConnectionIdAndCustomerIdAndInstitutionId(
                        connectionId,
                        request.customerId(),
                        request.institutionId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Connection not found"
                ));

        if (!"ACTIVE".equalsIgnoreCase(connection.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Connection is not active"
            );
        }

        consentAuthorizationService.requireConsent(
                connection.getCustomer().getCustomerId(),
                connection.getInstitution().getInstitutionId(),
                "CASH_FLOW"
        );

        String syncMode = request.syncMode() == null
                ? "INCREMENTAL"
                : request.syncMode().trim().toUpperCase();

        if (!syncMode.equals("FULL") && !syncMode.equals("INCREMENTAL")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "sync_mode must be FULL or INCREMENTAL"
            );
        }

        String providerCode = resolveProviderCode(
            connection.getInstitution().getName()
        );

        ConnectorProvider provider;

        try {
            provider = providerRegistry.getProvider(providerCode);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED,
                    "No connector provider is configured for institution: "
                            + providerCode
            );
        }

        ConnectorProvider.ConnectorSyncContext context =
                new ConnectorProvider.ConnectorSyncContext(
                        connection.getConnectionId(),
                        connection.getCustomer().getCustomerId(),
                        connection.getInstitution().getInstitutionId(),
                        connection.getProviderRef(),
                        syncMode,
                        connection.getLastSyncAt()
                );

        List<ConnectorProvider.ProviderTransaction> providerTransactions =
                provider.fetchTransactions(context);

        int recordsIngested = providerTransactions.size();
        int recordsNormalized = 0;
        int recordsQuarantined = 0;

        for (ConnectorProvider.ProviderTransaction providerTransaction
                : providerTransactions) {

            if (!isValid(providerTransaction)) {
                recordsQuarantined++;
                continue;
            }

            Account account = resolveAccount(
                    providerTransaction.accountRefToken(),
                    connection
            );

            ledgerService.recordProviderTransaction(account, providerTransaction);

            recordsNormalized++;
        }

        Instant syncedAt = Instant.now();
        connection.setLastSyncAt(syncedAt);
        connectionRepository.save(connection);

        String syncId = UUID.randomUUID().toString();

        auditService.logEvent(
                "APPLICATION",
                authenticatedInstitutionId.toString(),
                "CONNECTOR_SYNC",
                "CONNECTION",
                connectionId.toString(),
                syncId,
                "Connector sync completed in "
                        + syncMode
                        + " mode; provider="
                        + providerCode
                        + ", ingested="
                        + recordsIngested
                        + ", normalized="
                        + recordsNormalized
                        + ", quarantined="
                        + recordsQuarantined
        );

        return new ConnectorSyncResponse(
                syncId,
                "COMPLETED",
                recordsIngested,
                recordsNormalized,
                recordsQuarantined,
                syncedAt
        );
    }

    private boolean isValid(
            ConnectorProvider.ProviderTransaction transaction
    ) {
        return transaction != null
                && transaction.accountRefToken() != null
                && !transaction.accountRefToken().isBlank()
                && transaction.sourceEventId() != null
                && !transaction.sourceEventId().isBlank()
                && transaction.direction() != null
                && transaction.amount() != null
                && transaction.amount().signum() >= 0;
    }

    private Account resolveAccount(
            String accountRefToken,
            Connection connection
    ) {
        Account account = accountRepository
                .findByAccountRefToken(accountRefToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Provider account could not be mapped to a TAMVA account"
                ));

        UUID customerId = connection.getCustomer().getCustomerId();
        UUID institutionId = connection.getInstitution().getInstitutionId();

        if (!customerId.equals(account.getCustomer().getCustomerId())
                || !institutionId.equals(account.getInstitution().getInstitutionId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Provider account does not belong to the connector customer and institution"
            );
        }

        return account;
    }

    private String resolveProviderCode(String institutionName) {
        if (institutionName == null || institutionName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED,
                    "Connector institution is not configured"
            );
        }

        String normalized = institutionName.trim().toUpperCase();

        if (normalized.contains("GCB")) {
            return "GCB";
        }

        if (normalized.contains("MTN")) {
            return "MTN";
        }

        if (normalized.contains("TELECEL")) {
            return "TELECEL";
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "No connector provider is configured for institution: "
                        + institutionName
        );
    }
}