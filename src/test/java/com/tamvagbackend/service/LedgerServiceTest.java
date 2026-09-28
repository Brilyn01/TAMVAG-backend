package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Institution;
import com.tamvagbackend.domain.entity.LedgerEntry;
import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.domain.repository.AccountRepository;
import com.tamvagbackend.domain.repository.LedgerEntryRepository;
import com.tamvagbackend.domain.repository.TransactionRepository;
import com.tamvagbackend.service.connector.ConnectorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private TransactionNormalizationService normalizationService;

    private LedgerService ledgerService;

    private Account account;
    private Institution institution;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(
                transactionRepository,
                ledgerEntryRepository,
                accountRepository,
                auditService,
                normalizationService
        );

        UUID institutionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        institution = new Institution();
        institution.setInstitutionId(institutionId);
        institution.setName("GCB Bank");
        institution.setType("BANK");
        institution.setStatus("ACTIVE");

        account = new Account();
        account.setAccountId(accountId);
        account.setInstitution(institution);
        account.setAccountRefToken("GCB_ACC_987654321");
        account.setStatus("ACTIVE");
    }

    @Test
    void recordProviderTransactionShouldNormalizeAndPersistTransaction() {
        Instant occurredAt = Instant.parse("2026-09-15T10:15:30Z");

        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "GCB-PILOT-001",
                        "in",
                        new BigDecimal("2500.0000"),
                        "ghs",
                        occurredAt,
                        "BANK_CURRENT",
                        "GCB PILOT PAYROLL",
                        "SALARY",
                        "Salary payment",
                        "GCB"
                );

        Transaction normalizedTransaction = new Transaction();
        normalizedTransaction.setTransactionId(UUID.randomUUID());
        normalizedTransaction.setAccount(account);
        normalizedTransaction.setSourceEventId("GCB-PILOT-001");
        normalizedTransaction.setDirection("IN");
        normalizedTransaction.setAmount(new BigDecimal("2500"));
        normalizedTransaction.setCurrency("GHS");
        normalizedTransaction.setOccurredAt(occurredAt);
        normalizedTransaction.setStatus("NORMALISED");
        normalizedTransaction.setChannel("BANK_CURRENT");
        normalizedTransaction.setCounterparty("GCB PILOT PAYROLL");
        normalizedTransaction.setMerchantCategory("SALARY");
        normalizedTransaction.setReference("Salary payment");
        normalizedTransaction.setSourceSystem("GCB");
        normalizedTransaction.setNormalisationVersion(
                TransactionNormalizationService.NORMALISATION_VERSION
        );
        normalizedTransaction.setCreatedAt(Instant.now());

        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(normalizedTransaction.getTransactionId());
        savedTransaction.setAccount(account);
        savedTransaction.setSourceEventId("GCB-PILOT-001");
        savedTransaction.setDirection("IN");
        savedTransaction.setAmount(new BigDecimal("2500"));
        savedTransaction.setCurrency("GHS");
        savedTransaction.setOccurredAt(occurredAt);
        savedTransaction.setStatus("COMPLETED");
        savedTransaction.setChannel("BANK_CURRENT");
        savedTransaction.setCounterparty("GCB PILOT PAYROLL");
        savedTransaction.setMerchantCategory("SALARY");
        savedTransaction.setReference("Salary payment");
        savedTransaction.setSourceSystem("GCB");
        savedTransaction.setNormalisationVersion(
                TransactionNormalizationService.NORMALISATION_VERSION
        );
        savedTransaction.setCreatedAt(normalizedTransaction.getCreatedAt());

        when(normalizationService.normalize(account, providerTransaction))
                .thenReturn(normalizedTransaction);

        when(transactionRepository.findByAccountAndSourceSystemAndSourceEventId(
                account,
                "GCB",
                "GCB-PILOT-001"
        )).thenReturn(Optional.empty());

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result =
                ledgerService.recordProviderTransaction(
                        account,
                        providerTransaction
                );

        assertNotNull(result);
        assertEquals(savedTransaction.getTransactionId(), result.getTransactionId());
        assertEquals("GCB-PILOT-001", result.getSourceEventId());
        assertEquals("IN", result.getDirection());
        assertEquals(new BigDecimal("2500"), result.getAmount());
        assertEquals("GHS", result.getCurrency());
        assertEquals("BANK_CURRENT", result.getChannel());
        assertEquals("GCB PILOT PAYROLL", result.getCounterparty());
        assertEquals("SALARY", result.getMerchantCategory());
        assertEquals("Salary payment", result.getReference());
        assertEquals("GCB", result.getSourceSystem());
        assertEquals(
                TransactionNormalizationService.NORMALISATION_VERSION,
                result.getNormalisationVersion()
        );
        assertEquals("COMPLETED", result.getStatus());

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction persistedTransaction = transactionCaptor.getValue();

        assertEquals(account, persistedTransaction.getAccount());
        assertEquals("GCB-PILOT-001", persistedTransaction.getSourceEventId());
        assertEquals("IN", persistedTransaction.getDirection());
        assertEquals(new BigDecimal("2500"), persistedTransaction.getAmount());
        assertEquals("GHS", persistedTransaction.getCurrency());
        assertEquals("BANK_CURRENT", persistedTransaction.getChannel());
        assertEquals("GCB PILOT PAYROLL", persistedTransaction.getCounterparty());
        assertEquals("SALARY", persistedTransaction.getMerchantCategory());
        assertEquals("Salary payment", persistedTransaction.getReference());
        assertEquals("GCB", persistedTransaction.getSourceSystem());
        assertEquals(
                TransactionNormalizationService.NORMALISATION_VERSION,
                persistedTransaction.getNormalisationVersion()
        );
        assertEquals("COMPLETED", persistedTransaction.getStatus());

        verify(ledgerEntryRepository, atLeastOnce()).save(any(LedgerEntry.class));
    }

    @Test
    void recordProviderTransactionShouldCreateCreditLedgerEntryForIncomingTransaction() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "GCB-IN-001",
                        "IN",
                        new BigDecimal("1000.00"),
                        "GHS",
                        Instant.now(),
                        "BANK_CURRENT",
                        "EMPLOYER",
                        "SALARY",
                        "Salary",
                        "GCB"
                );

        Transaction normalizedTransaction = buildTransaction(
                "GCB-IN-001",
                "IN",
                new BigDecimal("1000.00"),
                "GHS",
                "SALARY",
                "Salary",
                "GCB"
        );

        Transaction savedTransaction = normalizedTransaction;
        savedTransaction.setStatus("COMPLETED");

        when(normalizationService.normalize(account, providerTransaction))
                .thenReturn(normalizedTransaction);

        when(transactionRepository.findByAccountAndSourceSystemAndSourceEventId(
                account,
                "GCB",
                "GCB-IN-001"
        )).thenReturn(Optional.empty());

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ledgerService.recordProviderTransaction(
                account,
                providerTransaction
        );

        ArgumentCaptor<LedgerEntry> entryCaptor =
                ArgumentCaptor.forClass(LedgerEntry.class);

        verify(ledgerEntryRepository, times(2)).save(entryCaptor.capture());

        LedgerEntry entry = entryCaptor.getAllValues().get(0);

        assertEquals(savedTransaction, entry.getTransaction());
        assertEquals("INCOME", entry.getEntryType());
        assertEquals(new BigDecimal("1000.00"), entry.getAmount());
        assertEquals("GHS", entry.getCurrency());
        assertEquals("CREDIT", entry.getBalanceEffect());
        assertEquals("GCB", entry.getCounterpartyId());
        assertEquals(BigDecimal.ONE, entry.getConfidence());
        assertNotNull(entry.getCreatedAt());
    }

    @Test
    void recordProviderTransactionShouldCreateDebitLedgerEntryForOutgoingTransaction() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "GCB-OUT-001",
                        "OUT",
                        new BigDecimal("125.00"),
                        "GHS",
                        Instant.now(),
                        "BANK_CURRENT",
                        "GCB",
                        "FEE",
                        "GCB account service fee",
                        "GCB"
                );

        Transaction normalizedTransaction = buildTransaction(
                "GCB-OUT-001",
                "OUT",
                new BigDecimal("125.00"),
                "GHS",
                "FEE",
                "GCB account service fee",
                "GCB"
        );

        normalizedTransaction.setCounterparty("GCB");

        Transaction savedTransaction = normalizedTransaction;
        savedTransaction.setStatus("COMPLETED");

        when(normalizationService.normalize(account, providerTransaction))
                .thenReturn(normalizedTransaction);

        when(transactionRepository.findByAccountAndSourceSystemAndSourceEventId(
                account,
                "GCB",
                "GCB-OUT-001"
        )).thenReturn(Optional.empty());

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ledgerService.recordProviderTransaction(
                account,
                providerTransaction
        );

        ArgumentCaptor<LedgerEntry> entryCaptor =
                ArgumentCaptor.forClass(LedgerEntry.class);

        verify(ledgerEntryRepository, times(2)).save(entryCaptor.capture());

        LedgerEntry entry = entryCaptor.getAllValues().get(0);

        assertEquals(savedTransaction, entry.getTransaction());
        assertEquals("FEE", entry.getEntryType());
        assertEquals(new BigDecimal("125.00"), entry.getAmount());
        assertEquals("GHS", entry.getCurrency());
        assertEquals("DEBIT", entry.getBalanceEffect());
        assertEquals("GCB", entry.getCounterpartyId());
        assertEquals(BigDecimal.ONE, entry.getConfidence());
        assertNotNull(entry.getCreatedAt());
    }

    @Test
    void recordProviderTransactionShouldReturnExistingTransactionForDuplicateEvent() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "GCB-DUP-001",
                        "IN",
                        new BigDecimal("500.00"),
                        "GHS",
                        Instant.now(),
                        "BANK_CURRENT",
                        "GCB",
                        "SALARY",
                        "Salary",
                        "GCB"
                );

        Transaction existingTransaction = buildTransaction(
                "GCB-DUP-001",
                "IN",
                new BigDecimal("500.00"),
                "GHS",
                "SALARY",
                "Salary",
                "GCB"
        );

        existingTransaction.setStatus("COMPLETED");

        when(normalizationService.normalize(account, providerTransaction))
                .thenReturn(existingTransaction);

        when(transactionRepository.findByAccountAndSourceSystemAndSourceEventId(
                account,
                "GCB",
                "GCB-DUP-001"
        )).thenReturn(Optional.of(existingTransaction));

        Transaction result =
                ledgerService.recordProviderTransaction(
                        account,
                        providerTransaction
                );

        assertSame(existingTransaction, result);

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void recordProviderTransactionShouldRejectNullAccount() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "GCB-001",
                        "IN",
                        new BigDecimal("100.00"),
                        "GHS",
                        Instant.now(),
                        "BANK_CURRENT",
                        "GCB",
                        null,
                        "Deposit",
                        "GCB"
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.recordProviderTransaction(
                        null,
                        providerTransaction
                )
        );

        assertEquals("Account is required", exception.getMessage());

        verifyNoInteractions(normalizationService);
        verifyNoInteractions(transactionRepository);
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void recordProviderTransactionShouldRejectNullProviderTransaction() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.recordProviderTransaction(
                        account,
                        null
                )
        );

        assertEquals(
                "Provider transaction is required",
                exception.getMessage()
        );

        verifyNoInteractions(normalizationService);
        verifyNoInteractions(transactionRepository);
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void recordProviderTransactionShouldPassCompleteProviderTransactionToNormalizer() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "GCB-COMPLETE-001",
                        "OUT",
                        new BigDecimal("75.00"),
                        "GHS",
                        Instant.parse("2026-09-15T12:00:00Z"),
                        "MOBILE_MONEY",
                        "MTN",
                        "SERVICE_FEE",
                        "MTN mobile money service fee",
                        "MTN"
                );

        Transaction normalizedTransaction = buildTransaction(
                "GCB-COMPLETE-001",
                "OUT",
                new BigDecimal("75.00"),
                "GHS",
                "SERVICE_FEE",
                "MTN mobile money service fee",
                "MTN"
        );

        normalizedTransaction.setChannel("MOBILE_MONEY");
        normalizedTransaction.setCounterparty("MTN");
        normalizedTransaction.setMerchantCategory("SERVICE_FEE");

        when(normalizationService.normalize(account, providerTransaction))
                .thenReturn(normalizedTransaction);

        when(transactionRepository.findByAccountAndSourceSystemAndSourceEventId(
                account,
                "MTN",
                "GCB-COMPLETE-001"
        )).thenReturn(Optional.empty());

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(normalizedTransaction);

        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ledgerService.recordProviderTransaction(
                account,
                providerTransaction
        );

        verify(normalizationService).normalize(
                account,
                providerTransaction
        );
    }

    @Test
    void recordProviderTransactionShouldTreatSameProviderAndEventAsDuplicate() {
        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "EVENT-001",
                        "IN",
                        new BigDecimal("500.00"),
                        "GHS",
                        Instant.now(),
                        "BANK_CURRENT",
                        "GCB",
                        "SALARY",
                        "Salary",
                        "GCB"
                );

        Transaction existingTransaction = buildTransaction(
                "EVENT-001",
                "IN",
                new BigDecimal("500.00"),
                "GHS",
                "SALARY",
                "Salary",
                "GCB"
        );

        existingTransaction.setStatus("COMPLETED");

        when(normalizationService.normalize(account, providerTransaction))
                .thenReturn(existingTransaction);

        when(transactionRepository.findByAccountAndSourceSystemAndSourceEventId(
                account,
                "GCB",
                "EVENT-001"
        )).thenReturn(Optional.of(existingTransaction));

        Transaction result =
                ledgerService.recordProviderTransaction(
                        account,
                        providerTransaction
                );

        assertSame(existingTransaction, result);

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void recordProviderTransactionShouldAllowSameEventIdFromDifferentProviders() {
        ConnectorProvider.ProviderTransaction gcbTransaction =
                new ConnectorProvider.ProviderTransaction(
                        "GCB_ACC_987654321",
                        "EVENT-001",
                        "IN",
                        new BigDecimal("500.00"),
                        "GHS",
                        Instant.now(),
                        "BANK_CURRENT",
                        "GCB",
                        "SALARY",
                        "GCB Salary",
                        "GCB"
                );

        Transaction normalizedTransaction = buildTransaction(
                "EVENT-001",
                "IN",
                new BigDecimal("500.00"),
                "GHS",
                "SALARY",
                "GCB Salary",
                "GCB"
        );

        normalizedTransaction.setStatus("NORMALISED");

        when(normalizationService.normalize(account, gcbTransaction))
                .thenReturn(normalizedTransaction);

        when(transactionRepository.findByAccountAndSourceSystemAndSourceEventId(
                account,
                "GCB",
                "EVENT-001"
        )).thenReturn(Optional.empty());

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(normalizedTransaction);

        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result =
                ledgerService.recordProviderTransaction(
                        account,
                        gcbTransaction
                );

        assertNotNull(result);

        verify(transactionRepository).save(any(Transaction.class));
        verify(ledgerEntryRepository, atLeastOnce()).save(any(LedgerEntry.class));
    }

    @Test
    void reverseTransactionShouldCreateReversalTransactionAndBalancedEntries() {
        UUID originalTxId = UUID.randomUUID();
        Transaction originalTx = buildTransaction("TX-100", "IN", new BigDecimal("500.00"), "GHS", "DEPOSIT", "Salary", "GCB");
        originalTx.setTransactionId(originalTxId);
        originalTx.setStatus("COMPLETED");

        when(transactionRepository.findById(originalTxId)).thenReturn(Optional.of(originalTx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction reversalResult = ledgerService.reverseTransaction(originalTxId, "Customer disputed charge");

        assertNotNull(reversalResult);
        assertEquals("OUT", reversalResult.getDirection());
        assertEquals(new BigDecimal("500.00"), reversalResult.getAmount());
        assertTrue(reversalResult.getReference().contains("REVERSAL: Customer disputed charge"));
        assertEquals("REVERSED", originalTx.getStatus());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
        verify(auditService).logEvent(eq("SYSTEM"), eq("system"), eq("TRANSACTION_REVERSED"), eq("TRANSACTION"), anyString(), any(), anyString());
    }

    @Test
    void reverseTransactionShouldFailIfAlreadyReversed() {
        UUID originalTxId = UUID.randomUUID();
        Transaction originalTx = buildTransaction("TX-101", "IN", new BigDecimal("200.00"), "GHS", "DEPOSIT", "Deposit", "GCB");
        originalTx.setTransactionId(originalTxId);
        originalTx.setStatus("REVERSED");

        when(transactionRepository.findById(originalTxId)).thenReturn(Optional.of(originalTx));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ledgerService.reverseTransaction(originalTxId, "Duplicate attempt")
        );

        assertTrue(exception.getMessage().contains("already reversed"));
    }

    private Transaction buildTransaction(
            String sourceEventId,
            String direction,
            BigDecimal amount,
            String currency,
            String merchantCategory,
            String reference,
            String sourceSystem
    ) {
        Transaction transaction = new Transaction();

        transaction.setTransactionId(UUID.randomUUID());
        transaction.setAccount(account);
        transaction.setSourceEventId(sourceEventId);
        transaction.setDirection(direction);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setOccurredAt(Instant.now());
        transaction.setStatus("NORMALISED");
        transaction.setChannel("BANK_CURRENT");
        transaction.setCounterparty("GCB");
        transaction.setMerchantCategory(merchantCategory);
        transaction.setReference(reference);
        transaction.setSourceSystem(sourceSystem);
        transaction.setNormalisationVersion(
                TransactionNormalizationService.NORMALISATION_VERSION
        );
        transaction.setCreatedAt(Instant.now());

        return transaction;
    }
}