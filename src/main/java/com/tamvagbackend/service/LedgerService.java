package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.*;
import com.tamvagbackend.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;

    public LedgerService(
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AccountRepository accountRepository,
            AuditService auditService
    ) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

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
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        // Idempotency check
        Optional<Transaction> existing = transactionRepository.findByAccountAndSourceEventId(account, sourceEventId);
        if (existing.isPresent()) {
            log.info("Duplicate transaction detected for account {} and sourceEventId {}", accountId, sourceEventId);
            return existing.get();
        }

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setSourceEventId(sourceEventId);
        tx.setDirection(direction.toUpperCase());
        tx.setAmount(amount.abs());
        tx.setCurrency(currency != null ? currency.toUpperCase() : "GHS");
        tx.setOccurredAt(occurredAt != null ? occurredAt : Instant.now());
        tx.setStatus("COMPLETED");
        tx.setChannel(channel);
        tx.setCounterparty(counterparty);
        tx.setReference(reference);
        tx.setSourceSystem(sourceSystem);
        tx.setNormalisationVersion("1.0.0");
        tx.setCreatedAt(Instant.now());

        Transaction savedTx = transactionRepository.save(tx);

        // Classify and create double-entry inspired ledger entry
        String entryType = classifyTransaction(account, tx);
        String balanceEffect = "IN".equalsIgnoreCase(direction) ? "CREDIT" : "DEBIT";

        LedgerEntry entry = new LedgerEntry();
        entry.setTransaction(savedTx);
        entry.setEntryType(entryType);
        entry.setAmount(savedTx.getAmount());
        entry.setCurrency(savedTx.getCurrency());
        entry.setBalanceEffect(balanceEffect);
        entry.setCounterpartyId(counterparty);
        entry.setConfidence(BigDecimal.ONE);
        entry.setCreatedAt(Instant.now());

        ledgerEntryRepository.save(entry);

        return savedTx;
    }

    private String classifyTransaction(Account account, Transaction tx) {
        String ref = (tx.getReference() != null ? tx.getReference().toLowerCase() : "");
        String channel = (tx.getChannel() != null ? tx.getChannel().toLowerCase() : "");

        if ("IN".equalsIgnoreCase(tx.getDirection())) {
            if (ref.contains("salary") || ref.contains("payroll") || ref.contains("deposit")) {
                return "INCOME";
            }
            if (ref.contains("transfer") || ref.contains("momo") || ref.contains("send")) {
                return "TRANSFER";
            }
            if (ref.contains("invest") || ref.contains("dividend")) {
                return "INVESTMENT";
            }
            return "INCOME";
        } else {
            if (ref.contains("loan") || ref.contains("repay") || ref.contains("debt")) {
                return "DEBT";
            }
            if (ref.contains("save") || ref.contains("susu") || ref.contains("stash")) {
                return "SAVING";
            }
            if (ref.contains("fee") || ref.contains("charge") || ref.contains("tax")) {
                return "FEE";
            }
            if (ref.contains("transfer") || ref.contains("cashout")) {
                return "TRANSFER";
            }
            return "EXPENSE";
        }
    }
}
