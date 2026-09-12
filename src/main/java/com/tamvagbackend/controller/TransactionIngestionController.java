package com.tamvagbackend.controller;

import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.dto.TransactionEventDtos.IngestionAckResponse;
import com.tamvagbackend.dto.TransactionEventDtos.TransactionIngestionEvent;
import com.tamvagbackend.service.LedgerService;
import com.tamvagbackend.service.TransactionEventProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/transactions")
@Tag(name = "Transaction Ingestion & Streaming", description = "Asynchronous Kafka event ingestion and synchronous ledger ingestion")
public class TransactionIngestionController {

    private final TransactionEventProducer transactionEventProducer;
    private final LedgerService ledgerService;

    public TransactionIngestionController(TransactionEventProducer transactionEventProducer, LedgerService ledgerService) {
        this.transactionEventProducer = transactionEventProducer;
        this.ledgerService = ledgerService;
    }

    @PostMapping("/async")
    @Operation(summary = "Async transaction ingestion via Kafka", description = "Streams financial activity events into Kafka topic for decoupled, asynchronous normalization, ledger processing, and feature computation")
    public ResponseEntity<IngestionAckResponse> ingestTransactionAsync(@Valid @RequestBody TransactionIngestionEvent event) {
        IngestionAckResponse ack = transactionEventProducer.publishTransactionEvent(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ack);
    }

    @PostMapping("/sync")
    @Operation(summary = "Synchronous transaction ingestion", description = "Immediately normalizes and records transaction into TAMVA PostgreSQL ledger with idempotency checks")
    public ResponseEntity<Transaction> ingestTransactionSync(@Valid @RequestBody TransactionIngestionEvent event) {
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
}
