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
    private final TransactionQuarantineService quarantineService;

    public ConnectorService(
            ConnectionRepository connectionRepository,
            AccountRepository accountRepository,
            ConsentAuthorizationService consentAuthorizationService,
            ConnectorProviderRegistry providerRegistry,
            LedgerService ledgerService,
            AuditService auditService,
            TransactionQuarantineService quarantineService
    ) {
        this.connectionRepository = connectionRepository;
        this.accountRepository = accountRepository;
        this.consentAuthorizationService = consentAuthorizationService;
        this.providerRegistry = providerRegistry;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
        this.quarantineService = quarantineService;
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

            /*
             * Validate the provider record first.
             *
             * Invalid provider data is quarantined and does not prevent
             * subsequent provider records from being processed.
             */
            String validationError = validate(providerTransaction);

            if (validationError != null) {
                quarantineService.quarantine(
                        connection,
                        providerTransaction,
                        validationError
                );

                recordsQuarantined++;
                continue;
            }

            /*
             * Account mapping is also record-level ingestion validation.
             *
             * An unmapped or incorrectly owned account must not abort the
             * entire connector sync. Instead, quarantine that provider
             * record and continue with the remaining records.
             */
            try {
                Account account = resolveAccount(
                        providerTransaction.accountRefToken(),
                        connection
                );

                ledgerService.recordProviderTransaction(
                        account,
                        providerTransaction
                );

                recordsNormalized++;

            } catch (ResponseStatusException ex) {

                String quarantineReason;

                if (ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                    quarantineReason =
                            "Provider account could not be mapped to a TAMVA account";
                } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                    quarantineReason =
                            "Provider account does not belong to the connector customer and institution";
                } else {
                    throw ex;
                }

                quarantineService.quarantine(
                        connection,
                        providerTransaction,
                        quarantineReason
                );

                recordsQuarantined++;
            }
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

    private String validate(
            ConnectorProvider.ProviderTransaction transaction
    ) {
        if (transaction == null) {
            return "Provider transaction is null";
        }

        if (transaction.accountRefToken() == null
                || transaction.accountRefToken().isBlank()) {
            return "Provider account reference is required";
        }

        if (transaction.sourceEventId() == null
                || transaction.sourceEventId().isBlank()) {
            return "Provider source event ID is required";
        }

        if (transaction.direction() == null
                || transaction.direction().isBlank()) {
            return "Provider transaction direction is required";
        }

        if (!"IN".equalsIgnoreCase(transaction.direction())
                && !"OUT".equalsIgnoreCase(transaction.direction())) {
            return "Provider transaction direction must be IN or OUT";
        }

        if (transaction.amount() == null) {
            return "Provider transaction amount is required";
        }

        if (transaction.amount().signum() < 0) {
            return "Provider transaction amount cannot be negative";
        }

        if (transaction.currency() == null
                || transaction.currency().isBlank()) {
            return "Provider transaction currency is required";
        }

        if (transaction.currency().trim().length() != 3) {
            return "Provider transaction currency must be a 3-letter ISO code";
        }

        return null;
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