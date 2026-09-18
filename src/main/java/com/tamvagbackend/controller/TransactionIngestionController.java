package com.tamvagbackend.controller;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.domain.repository.AccountRepository;
import com.tamvagbackend.dto.TransactionEventDtos.IngestionAckResponse;
import com.tamvagbackend.dto.TransactionEventDtos.TransactionIngestionEvent;
import com.tamvagbackend.service.ConsentAuthorizationService;
import com.tamvagbackend.service.LedgerService;
import com.tamvagbackend.service.TransactionEventProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/transactions")
@Tag(
        name = "Transaction Ingestion & Streaming",
        description = "Asynchronous Kafka event ingestion and synchronous ledger ingestion"
)
public class TransactionIngestionController {

    private final TransactionEventProducer transactionEventProducer;
    private final LedgerService ledgerService;
    private final AccountRepository accountRepository;
    private final ConsentAuthorizationService consentAuthorizationService;

    public TransactionIngestionController(
            TransactionEventProducer transactionEventProducer,
            LedgerService ledgerService,
            AccountRepository accountRepository,
            ConsentAuthorizationService consentAuthorizationService
    ) {
        this.transactionEventProducer = transactionEventProducer;
        this.ledgerService = ledgerService;
        this.accountRepository = accountRepository;
        this.consentAuthorizationService = consentAuthorizationService;
    }

    @PostMapping("/async")
    @PreAuthorize("hasAuthority('SCOPE_transaction:write')")
    @Operation(
            summary = "Async transaction ingestion via Kafka",
            description = "Streams financial activity events into Kafka topic for decoupled, asynchronous normalization, ledger processing, and feature computation"
    )
    public ResponseEntity<IngestionAckResponse> ingestTransactionAsync(
            @Valid @RequestBody TransactionIngestionEvent event,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Account account = authorizeTransactionAccess(event.accountId(), jwt);

        consentAuthorizationService.requireConsent(
                account.getCustomer().getCustomerId(),
                account.getInstitution().getInstitutionId(),
                "transaction:write"
        );

        IngestionAckResponse ack =
                transactionEventProducer.publishTransactionEvent(event);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ack);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('SCOPE_transaction:write')")
    @Operation(
            summary = "Synchronous transaction ingestion",
            description = "Immediately normalizes and records transaction into TAMVA PostgreSQL ledger with idempotency checks"
    )
    public ResponseEntity<Transaction> ingestTransactionSync(
            @Valid @RequestBody TransactionIngestionEvent event,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Account account = authorizeTransactionAccess(event.accountId(), jwt);

        consentAuthorizationService.requireConsent(
                account.getCustomer().getCustomerId(),
                account.getInstitution().getInstitutionId(),
                "transaction:write"
        );

        Transaction tx = ledgerService.recordTransaction(
                event.accountId(),
                event.sourceEventId(),
                event.direction(),
                event.amount(),
                event.currency() != null ? event.currency() : "GHS",
                event.occurredAt(),
                event.channel(),
                event.counterparty(),
                event.reference(),
                event.sourceSystem()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(tx);
    }

    private Account authorizeTransactionAccess(
            UUID accountId,
            Jwt jwt
    ) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found: " + accountId
                ));

        UUID authenticatedInstitutionId = UUID.fromString(
                jwt.getClaimAsString("institution_id")
        );

        UUID accountInstitutionId =
                account.getInstitution().getInstitutionId();

        if (!authenticatedInstitutionId.equals(accountInstitutionId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account does not belong to the authenticated institution"
            );
        }

        return account;
    }
}
