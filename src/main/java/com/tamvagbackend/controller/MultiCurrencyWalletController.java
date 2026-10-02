package com.tamvagbackend.controller;

import com.tamvagbackend.dto.MultiCurrencyDtos.*;
import com.tamvagbackend.service.MultiCurrencyWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/v1/wallet")
@Tag(
        name = "Dynamic Multi-Currency Wallet",
        description = "Multi-currency balances (GHS default, NGN, KES, ZAR, EGP, USD, GBP, EUR), real-time conversion matrix, and wallet transfers"
)
public class MultiCurrencyWalletController {

    private final MultiCurrencyWalletService walletService;

    public MultiCurrencyWalletController(
            MultiCurrencyWalletService walletService
    ) {
        this.walletService = walletService;
    }

    @GetMapping("/currencies")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Supported currencies & exchange rates",
            description = "Returns default GHS currency, 8 supported currencies with symbols/flags, and current GHS base rate matrix"
    )
    public ResponseEntity<SupportedCurrenciesResponse> getSupportedCurrencies() {

        return ResponseEntity.ok(
                walletService.getSupportedCurrencies()
        );
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAuthority('SCOPE_wallet:read')")
    @Operation(
            summary = "Get wallet details & balances",
            description = "Retrieve multi-currency balances for the authenticated user's customer wallet"
    )
    public ResponseEntity<MultiCurrencyWalletResponse> getWalletDetails(
            @PathVariable("customerId") UUID customerId,
            @RequestParam(
                    name = "currency",
                    required = false,
                    defaultValue = "GHS"
            ) String displayCurrency,
            Authentication authentication
    ) {

        Jwt jwt = extractJwt(authentication);

        UUID authenticatedCustomerId =
                extractUserCustomerId(jwt);

        requireCustomerOwnership(
                customerId,
                authenticatedCustomerId
        );

        return ResponseEntity.ok(
                walletService.getWalletDetails(
                        authenticatedCustomerId,
                        displayCurrency
                )
        );
    }

    @PostMapping("/convert")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get currency conversion quote",
            description = "Real-time currency conversion quote across any pair of supported currencies with spread and fee breakdown"
    )
    public ResponseEntity<CurrencyConvertResponse> getConversionQuote(
            @Valid
            @RequestBody
            CurrencyConvertRequest request
    ) {

        return ResponseEntity.ok(
                walletService.getConversionQuote(request)
        );
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('SCOPE_wallet:write')")
    @Operation(
            summary = "Execute multi-currency transfer",
            description = "Convert/swap balances in real-time between supported currencies in the authenticated user's wallet"
    )
    public ResponseEntity<CurrencyTransferResponse> executeTransfer(
            @Valid
            @RequestBody
            CurrencyTransferRequest request,
            Authentication authentication
    ) {

        Jwt jwt = extractJwt(authentication);

        UUID authenticatedCustomerId =
                extractUserCustomerId(jwt);

        requireCustomerOwnership(
                request.customerId(),
                authenticatedCustomerId
        );

        return ResponseEntity.ok(
                walletService.executeTransfer(
                        request,
                        authenticatedCustomerId
                )
        );
    }

    private Jwt extractJwt(Authentication authentication) {

        if (authentication == null
                || authentication.getPrincipal() == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            return jwt;
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid authentication principal"
        );
    }

    private UUID extractUserCustomerId(Jwt jwt) {

        String tokenType =
                jwt.getClaimAsString("token_type");

        if (!"user".equalsIgnoreCase(tokenType)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Wallet access requires a user token"
            );
        }

        String customerIdClaim =
                jwt.getClaimAsString("customer_id");

        if (customerIdClaim == null
                || customerIdClaim.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authenticated user is not linked to a customer profile"
            );
        }

        try {

            return UUID.fromString(customerIdClaim);

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid customer identity in access token"
            );
        }
    }

    private void requireCustomerOwnership(
            UUID requestedCustomerId,
            UUID authenticatedCustomerId
    ) {

        if (requestedCustomerId == null
                || authenticatedCustomerId == null
                || !requestedCustomerId.equals(authenticatedCustomerId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can access only your own customer wallet"
            );
        }
    }
}