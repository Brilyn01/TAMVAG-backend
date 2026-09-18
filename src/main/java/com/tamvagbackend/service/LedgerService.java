
package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.LedgerEntry;
import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.domain.repository.AccountRepository;
import com.tamvagbackend.domain.repository.LedgerEntryRepository;
import com.tamvagbackend.domain.repository.TransactionRepository;
import com.tamvagbackend.service.connector.ConnectorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class LedgerService {

    private static final Logger log =
            LoggerFactory.getLogger(LedgerService.class);

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final TransactionNormalizationService normalizationService;

    public LedgerService(
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AccountRepository accountRepository,
            AuditService auditService,
            TransactionNormalizationService normalizationService
    ) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
        this.normalizationService = normalizationService;
    }

    /**
     * Existing transaction-recording API.
     *
     * Kept for compatibility with existing callers.
     *
     * Normalization is now delegated to TransactionNormalizationService
     * instead of being duplicated inside LedgerService.
     */
    @Transactional
    public Transaction recordTransaction(
            UUID accountId,
            String sourceEventId,
            String direction,
            BigDecimal amount,
            String currency,
            Instant occurredAt,
            String channel,
            String counterparty,
            String reference,
            String sourceSystem
    ) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account not found: " + accountId
                        )
                );

        ConnectorProvider.ProviderTransaction providerTransaction =
                new ConnectorProvider.ProviderTransaction(
                        resolveAccountRefToken(account),
                        sourceEventId,
                        direction,
                        amount,
                        currency,
                        occurredAt,
                        channel,
                        counterparty,
                        null,
                        reference,
                        sourceSystem
                );

        return recordNormalizedTransaction(
                account,
                providerTransaction
        );
    }

    /**
     * Provider-oriented transaction recording API.
     *
     * This is the preferred entry point for connector ingestion because
     * it preserves all provider transaction fields, including
     * merchantCategory.
     */
    @Transactional
    public Transaction recordProviderTransaction(
            Account account,
            ConnectorProvider.ProviderTransaction providerTransaction
    ) {
        if (account == null) {
            throw new IllegalArgumentException("Account is required");
        }

        if (providerTransaction == null) {
            throw new IllegalArgumentException(
                    "Provider transaction is required"
            );
        }

        return recordNormalizedTransaction(
                account,
                providerTransaction
        );
    }

    /**
     * Normalizes, checks idempotency, persists the canonical transaction,
     * and creates its ledger entry.
     */
    private Transaction recordNormalizedTransaction(
            Account account,
            ConnectorProvider.ProviderTransaction providerTransaction
    ) {
        Transaction normalizedTransaction =
                normalizationService.normalize(
                        account,
                        providerTransaction
                );

        /*
         * Idempotency check.
         *
         * Provider-scoped idempotency will be strengthened in the
         * following Phase 4 step. For now, retain the existing database
         * constraint and repository lookup.
         */
        Optional<Transaction> existing =
            transactionRepository.findByAccountAndSourceSystemAndSourceEventId(
                    account,
                    normalizedTransaction.getSourceSystem(),
                    normalizedTransaction.getSourceEventId()
            );

        if (existing.isPresent()) {
            log.info(
                    "Duplicate transaction detected for account {} " +
                    "and sourceEventId {}",
                    account.getAccountId(),
                    normalizedTransaction.getSourceEventId()
            );

            return existing.get();
        }

        /*
         * A canonical transaction has been normalized successfully.
         *
         * The ledger layer owns persistence and changes the status to
         * COMPLETED once the transaction is accepted into the ledger.
         */
        normalizedTransaction.setStatus("COMPLETED");

        Transaction savedTx =
                transactionRepository.save(normalizedTransaction);

        /*
         * Classify and create double-entry inspired ledger entry.
         */
        String entryType =
                classifyTransaction(account, savedTx);

        String balanceEffect =
                "IN".equalsIgnoreCase(savedTx.getDirection())
                        ? "CREDIT"
                        : "DEBIT";

        LedgerEntry entry = new LedgerEntry();

        entry.setTransaction(savedTx);
        entry.setEntryType(entryType);
        entry.setAmount(savedTx.getAmount());
        entry.setCurrency(savedTx.getCurrency());
        entry.setBalanceEffect(balanceEffect);
        entry.setCounterpartyId(savedTx.getCounterparty());
        entry.setConfidence(BigDecimal.ONE);
        entry.setCreatedAt(Instant.now());

        ledgerEntryRepository.save(entry);

        return savedTx;
    }

    /**
     * Resolves the account reference used by the compatibility method.
     *
     * Provider ingestion normally supplies the accountRefToken directly,
     * so this fallback is only used by callers of the older
     * recordTransaction(...) API.
     */
    private String resolveAccountRefToken(Account account) {
        if (account.getAccountRefToken() == null
                || account.getAccountRefToken().isBlank()) {
            throw new IllegalArgumentException(
                    "Account reference token is required for transaction normalization"
            );
        }

        return account.getAccountRefToken();
    }

    private String classifyTransaction(
            Account account,
            Transaction tx
    ) {
        String ref =
                tx.getReference() != null
                        ? tx.getReference().toLowerCase()
                        : "";

        if ("IN".equalsIgnoreCase(tx.getDirection())) {

            if (ref.contains("salary")
                    || ref.contains("payroll")
                    || ref.contains("deposit")) {
                return "INCOME";
            }

            if (ref.contains("transfer")
                    || ref.contains("momo")
                    || ref.contains("send")) {
                return "TRANSFER";
            }

            if (ref.contains("invest")
                    || ref.contains("dividend")) {
                return "INVESTMENT";
            }

            return "INCOME";

        } else {

            if (ref.contains("loan")
                    || ref.contains("repay")
                    || ref.contains("debt")) {
                return "DEBT";
            }

            if (ref.contains("save")
                    || ref.contains("susu")
                    || ref.contains("stash")) {
                return "SAVING";
            }

            if (ref.contains("fee")
                    || ref.contains("charge")
                    || ref.contains("tax")) {
                return "FEE";
            }

            if (ref.contains("transfer")
                    || ref.contains("cashout")) {
                return "TRANSFER";
            }

            return "EXPENSE";
        }
    }
}
