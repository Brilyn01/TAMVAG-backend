package com.tamvagbackend.controller;

import com.tamvagbackend.dto.MultiCurrencyDtos.*;
import com.tamvagbackend.service.MultiCurrencyWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/wallet")
@Tag(name = "Dynamic Multi-Currency Wallet", description = "Multi-currency balances (GHS default, NGN, KES, ZAR, EGP, USD, GBP, EUR), real-time conversion matrix, and wallet transfers")
public class MultiCurrencyWalletController {

    private final MultiCurrencyWalletService walletService;

    public MultiCurrencyWalletController(MultiCurrencyWalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/currencies")
    @Operation(summary = "Supported currencies & exchange rates", description = "Returns default GHS currency, 8 supported currencies with symbols/flags, and current GHS base rate matrix")
    public ResponseEntity<SupportedCurrenciesResponse> getSupportedCurrencies() {
        return ResponseEntity.ok(walletService.getSupportedCurrencies());
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get wallet details & balances", description = "Retrieve multi-currency balances for a customer with total portfolio value converted into requested display currency (defaults to GHS)")
    public ResponseEntity<MultiCurrencyWalletResponse> getWalletDetails(
            @PathVariable("customerId") UUID customerId,
            @RequestParam(name = "currency", required = false, defaultValue = "GHS") String displayCurrency
    ) {
        return ResponseEntity.ok(walletService.getWalletDetails(customerId, displayCurrency));
    }

    @PostMapping("/convert")
    @Operation(summary = "Get currency conversion quote", description = "Real-time currency conversion quote across any pair of supported currencies with spread and fee breakdown")
    public ResponseEntity<CurrencyConvertResponse> getConversionQuote(@Valid @RequestBody CurrencyConvertRequest request) {
        return ResponseEntity.ok(walletService.getConversionQuote(request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Execute multi-currency transfer", description = "Convert/swap balances in real-time between supported currencies in customer wallet")
    public ResponseEntity<CurrencyTransferResponse> executeTransfer(@Valid @RequestBody CurrencyTransferRequest request) {
        return ResponseEntity.ok(walletService.executeTransfer(request));
    }
}
