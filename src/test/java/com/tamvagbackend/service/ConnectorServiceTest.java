package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Connection;
import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Institution;
import com.tamvagbackend.domain.repository.AccountRepository;
import com.tamvagbackend.domain.repository.ConnectionRepository;
import com.tamvagbackend.dto.AuditDtos.ConnectorSyncRequest;
import com.tamvagbackend.dto.AuditDtos.ConnectorSyncResponse;
import com.tamvagbackend.service.connector.ConnectorProvider;
import com.tamvagbackend.service.connector.ConnectorProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectorServiceTest {

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ConsentAuthorizationService consentAuthorizationService;

    @Mock
    private ConnectorProviderRegistry providerRegistry;

    @Mock
    private ConnectorProvider connectorProvider;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private AuditService auditService;

    private ConnectorService connectorService;

    private UUID connectionId;
    private UUID customerId;
    private UUID institutionId;
    private UUID accountId;

    private Connection connection;
    private Account account;

    @BeforeEach
    void setUp() {
        connectorService = new ConnectorService(
                connectionRepository,
                accountRepository,
                consentAuthorizationService,
                providerRegistry,
                ledgerService,
                auditService
        );

        connectionId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        Institution institution = new Institution();
        institution.setInstitutionId(institutionId);
        institution.setName("GCB Bank");
        institution.setType("BANK");
        institution.setStatus("ACTIVE");

        connection = new Connection();
        connection.setConnectionId(connectionId);
        connection.setCustomer(customer);
        connection.setInstitution(institution);
        connection.setStatus("ACTIVE");
        connection.setProviderRef("GCB_ACC_987654321");

        account = new Account();
        account.setAccountId(accountId);
        account.setCustomer(customer);
        account.setInstitution(institution);
        account.setAccountRefToken("GCB_ACC_987654321");
        account.setStatus("ACTIVE");
    }

    @Test
    void syncShouldProcessProviderTransactions() {
        ConnectorSyncRequest request = new ConnectorSyncRequest(
                customerId,
                institutionId,
                "FULL"
        );

        ConnectorProvider.ProviderTransaction transaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "GCB-PILOT-001",
                        "IN",
                        new BigDecimal("2500.00"),
                        "GHS",
                        Instant.now(),
                        "BANK_CURRENT",
                        "GCB PILOT PAYROLL",
                        null,
                        "SALARY",
                        "GCB"
                );

        when(connectionRepository
                .findByConnectionIdAndCustomerIdAndInstitutionId(
                        connectionId,
                        customerId,
                        institutionId
                ))
                .thenReturn(Optional.of(connection));

        when(providerRegistry.getProvider("GCB"))
                .thenReturn(connectorProvider);

        when(connectorProvider.fetchTransactions(any()))
                .thenReturn(List.of(transaction));

        when(accountRepository.findByAccountRefToken("GCB_ACC_987654321"))
                .thenReturn(Optional.of(account));

        when(consentAuthorizationService.requireConsent(
                customerId,
                institutionId,
                "CASH_FLOW"
        ))
                .thenReturn(null);

        when(connectionRepository.save(any(Connection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectorSyncResponse response =
                connectorService.sync(
                        connectionId,
                        request,
                        institutionId
                );

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.recordsIngested());
        assertEquals(1, response.recordsNormalized());
        assertEquals(0, response.recordsQuarantined());
        assertNotNull(response.syncId());
        assertNotNull(response.syncedAt());

        verify(connectorProvider).fetchTransactions(any());

        verify(ledgerService).recordProviderTransaction(
                eq(account),
                argThat(providerTransaction ->
                        "GCB-PILOT-001".equals(providerTransaction.sourceEventId())
                                && "IN".equals(providerTransaction.direction())
                                && new BigDecimal("2500.00").compareTo(providerTransaction.amount()) == 0
                                && "GHS".equals(providerTransaction.currency())
                                && "BANK_CURRENT".equals(providerTransaction.channel())
                                && "GCB PILOT PAYROLL".equals(providerTransaction.counterparty())
                                && "SALARY".equals(providerTransaction.reference())
                                && "GCB".equals(providerTransaction.sourceSystem())
                )
        );

        verify(connectionRepository).save(connection);

        verify(auditService).logEvent(
                eq("APPLICATION"),
                eq(institutionId.toString()),
                eq("CONNECTOR_SYNC"),
                eq("CONNECTION"),
                eq(connectionId.toString()),
                anyString(),
                contains("Connector sync completed")
        );
    }

    @Test
    void syncShouldQuarantineInvalidProviderTransaction() {
        ConnectorSyncRequest request = new ConnectorSyncRequest(
                customerId,
                institutionId,
                "FULL"
        );

        ConnectorProvider.ProviderTransaction invalidTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "",
                        "IN",
                        new BigDecimal("100.00"),
                        "GHS",
                        Instant.now(),
                        "BANK_CURRENT",
                        null,
                        null,
                        null,
                        "GCB"
                );

        when(connectionRepository
                .findByConnectionIdAndCustomerIdAndInstitutionId(
                        connectionId,
                        customerId,
                        institutionId
                ))
                .thenReturn(Optional.of(connection));

        when(providerRegistry.getProvider("GCB"))
                .thenReturn(connectorProvider);

        when(connectorProvider.fetchTransactions(any()))
                .thenReturn(List.of(invalidTransaction));

        when(consentAuthorizationService.requireConsent(
                customerId,
                institutionId,
                "CASH_FLOW"
        ))
                .thenReturn(null);

        when(connectionRepository.save(any(Connection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectorSyncResponse response =
                connectorService.sync(
                        connectionId,
                        request,
                        institutionId
                );

        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.recordsIngested());
        assertEquals(0, response.recordsNormalized());
        assertEquals(1, response.recordsQuarantined());

        verify(ledgerService, never()).recordProviderTransaction(
                any(Account.class),
                any(ConnectorProvider.ProviderTransaction.class)
        );
    }

    @Test
    void syncShouldRejectInstitutionMismatch() {
        UUID authenticatedInstitutionId = UUID.randomUUID();

        ConnectorSyncRequest request = new ConnectorSyncRequest(
                customerId,
                institutionId,
                "FULL"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> connectorService.sync(
                        connectionId,
                        request,
                        authenticatedInstitutionId
                )
        );

        assertEquals(403, exception.getStatusCode().value());
        verifyNoInteractions(providerRegistry);
        verifyNoInteractions(ledgerService);
    }

    @Test
    void syncShouldRejectMissingConnection() {
        ConnectorSyncRequest request = new ConnectorSyncRequest(
                customerId,
                institutionId,
                "FULL"
        );

        when(connectionRepository
                .findByConnectionIdAndCustomerIdAndInstitutionId(
                        connectionId,
                        customerId,
                        institutionId
                ))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> connectorService.sync(
                        connectionId,
                        request,
                        institutionId
                )
        );

        assertEquals(404, exception.getStatusCode().value());
        verifyNoInteractions(providerRegistry);
    }

    @Test
    void syncShouldRejectInactiveConnection() {
        connection.setStatus("INACTIVE");

        ConnectorSyncRequest request = new ConnectorSyncRequest(
                customerId,
                institutionId,
                "FULL"
        );

        when(connectionRepository
                .findByConnectionIdAndCustomerIdAndInstitutionId(
                        connectionId,
                        customerId,
                        institutionId
                ))
                .thenReturn(Optional.of(connection));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> connectorService.sync(
                        connectionId,
                        request,
                        institutionId
                )
        );

        assertEquals(403, exception.getStatusCode().value());
        verifyNoInteractions(providerRegistry);
    }

    @Test
    void syncShouldRejectInvalidSyncMode() {
        ConnectorSyncRequest request = new ConnectorSyncRequest(
                customerId,
                institutionId,
                "INVALID"
        );

        when(connectionRepository
                .findByConnectionIdAndCustomerIdAndInstitutionId(
                        connectionId,
                        customerId,
                        institutionId
                ))
                .thenReturn(Optional.of(connection));

        when(consentAuthorizationService.requireConsent(
                customerId,
                institutionId,
                "CASH_FLOW"
        ))
                .thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> connectorService.sync(
                        connectionId,
                        request,
                        institutionId
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(providerRegistry);
    }

    @Test
    void syncShouldRejectWhenProviderIsNotConfigured() {
        ConnectorSyncRequest request = new ConnectorSyncRequest(
                customerId,
                institutionId,
                "FULL"
        );

        when(connectionRepository
                .findByConnectionIdAndCustomerIdAndInstitutionId(
                        connectionId,
                        customerId,
                        institutionId
                ))
                .thenReturn(Optional.of(connection));

        when(consentAuthorizationService.requireConsent(
                customerId,
                institutionId,
                "CASH_FLOW"
        ))
                .thenReturn(null);

        when(providerRegistry.getProvider("GCB"))
                .thenThrow(new IllegalArgumentException(
                        "No connector provider registered for: GCB"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> connectorService.sync(
                        connectionId,
                        request,
                        institutionId
                )
        );

        assertEquals(501, exception.getStatusCode().value());
    }

    @Test
    void syncShouldDefaultToIncrementalMode() {
        ConnectorSyncRequest request = new ConnectorSyncRequest(
                customerId,
                institutionId,
                null
        );

        when(connectionRepository
                .findByConnectionIdAndCustomerIdAndInstitutionId(
                        connectionId,
                        customerId,
                        institutionId
                ))
                .thenReturn(Optional.of(connection));

        when(consentAuthorizationService.requireConsent(
                customerId,
                institutionId,
                "CASH_FLOW"
        ))
                .thenReturn(null);

        when(providerRegistry.getProvider("GCB"))
                .thenReturn(connectorProvider);

        when(connectorProvider.fetchTransactions(any()))
                .thenReturn(List.of());

        when(connectionRepository.save(any(Connection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectorSyncResponse response =
                connectorService.sync(
                        connectionId,
                        request,
                        institutionId
                );

        assertEquals("COMPLETED", response.status());
        assertEquals(0, response.recordsIngested());
        assertEquals(0, response.recordsNormalized());
        assertEquals(0, response.recordsQuarantined());

        verify(connectorProvider).fetchTransactions(
                argThat(context ->
                        "INCREMENTAL".equals(context.syncMode())
                )
        );
    }
}