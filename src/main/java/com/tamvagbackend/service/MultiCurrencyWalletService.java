package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.*;
import com.tamvagbackend.domain.repository.*;
import com.tamvagbackend.dto.MultiCurrencyDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MultiCurrencyWalletService {

    private static final Logger log = LoggerFactory.getLogger(MultiCurrencyWalletService.class);

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyTransferRepository currencyTransferRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    // Ordered list of 8 supported currencies according to specification
    public static final List<CurrencyMetadata> SUPPORTED_CURRENCIES = List.of(
            new CurrencyMetadata("GHS", "Ghanaian Cedi", "GH₵", "🇬🇭", true),
            new CurrencyMetadata("NGN", "Nigerian Naira", "₦", "🇳🇬", false),
            new CurrencyMetadata("KES", "Kenyan Shilling", "KSh", "🇰🇪", false),
            new CurrencyMetadata("ZAR", "South African Rand", "R", "🇿🇦", false),
            new CurrencyMetadata("EGP", "Egyptian Pound", "E£", "🇪🇬", false),
            new CurrencyMetadata("USD", "US Dollar", "$", "🇺🇸", false),
            new CurrencyMetadata("GBP", "British Pound", "£", "🇬🇧", false),
            new CurrencyMetadata("EUR", "Euro", "€", "🇪🇺", false)
    );

    public MultiCurrencyWalletService(
            WalletRepository walletRepository,
            WalletBalanceRepository walletBalanceRepository,
            ExchangeRateRepository exchangeRateRepository,
            CurrencyTransferRepository currencyTransferRepository,
            CustomerRepository customerRepository,
            AuditService auditService
    ) {
        this.walletRepository = walletRepository;
        this.walletBalanceRepository = walletBalanceRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.currencyTransferRepository = currencyTransferRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public SupportedCurrenciesResponse getSupportedCurrencies() {
        List<ExchangeRateDetail> rates = exchangeRateRepository.findByBaseCurrency("GHS").stream()
                .map(r -> new ExchangeRateDetail(
                        r.getBaseCurrency(),
                        r.getQuoteCurrency(),
                        r.getRate(),
                        r.getInverseRate(),
                        r.getSpreadPercentage(),
                        r.getEffectiveAt()
                ))
                .collect(Collectors.toList());

        return new SupportedCurrenciesResponse("GHS", SUPPORTED_CURRENCIES, rates);
    }

    @Transactional
    public Wallet getOrCreateWallet(Customer customer) {
        return walletRepository.findByCustomer(customer).orElseGet(() -> {
            Wallet newWallet = new Wallet(customer, "GHS");
            Wallet savedWallet = walletRepository.save(newWallet);

            // Initialize 8 currency balance accounts at ZERO (real wallet behavior)
            for (CurrencyMetadata meta : SUPPORTED_CURRENCIES) {
                WalletBalance balance =
                    new WalletBalance(savedWallet, meta.code(), BigDecimal.ZERO);

                savedWallet.getBalances().add(balance);
            }

            auditService.logEvent("SYSTEM", "system", "WALLET_CREATED", "WALLET", savedWallet.getWalletId().toString(), null, "Created multi-currency wallet for customer " + customer.getCustomerId());
            return savedWallet;
        });
    }

    @Transactional(readOnly = true)
    public MultiCurrencyWalletResponse getWalletDetails(UUID customerId, String displayCurrencyParam) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        Wallet wallet = getOrCreateWallet(customer);
        String displayCurrency = (displayCurrencyParam != null && isValidCurrency(displayCurrencyParam))
                ? displayCurrencyParam.toUpperCase()
                : wallet.getDefaultCurrency();

        List<WalletBalance> balances = walletBalanceRepository.findByWallet(wallet);
        Map<String, WalletBalance> balanceMap = balances.stream()
                .collect(Collectors.toMap(b -> b.getCurrency().toUpperCase(), b -> b));

        BigDecimal totalPortfolioValue = BigDecimal.ZERO;
        List<CurrencyBalanceItem> balanceItems = new ArrayList<>();

        for (CurrencyMetadata meta : SUPPORTED_CURRENCIES) {
            WalletBalance bal = balanceMap.get(meta.code());
            BigDecimal avail = bal != null ? bal.getAvailableAmount() : BigDecimal.ZERO;
            BigDecimal locked = bal != null ? bal.getLockedAmount() : BigDecimal.ZERO;

            BigDecimal convertedToDisplay = convertAmount(meta.code(), displayCurrency, avail);
            totalPortfolioValue = totalPortfolioValue.add(convertedToDisplay);

            balanceItems.add(new CurrencyBalanceItem(
                    meta.code(),
                    meta.symbol(),
                    meta.flagEmoji(),
                    avail.setScale(2, RoundingMode.HALF_UP),
                    locked.setScale(2, RoundingMode.HALF_UP),
                    convertedToDisplay.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        List<CurrencyTransferResponse> recentTransfers = currencyTransferRepository.findByWalletOrderByCreatedAtDesc(wallet).stream()
                .limit(10)
                .map(this::toTransferResponse)
                .collect(Collectors.toList());

        return new MultiCurrencyWalletResponse(
                wallet.getWalletId(),
                customer.getCustomerId(),
                wallet.getDefaultCurrency(),
                displayCurrency,
                totalPortfolioValue.setScale(2, RoundingMode.HALF_UP),
                balanceItems,
                recentTransfers
        );
    }

    @Transactional(readOnly = true)
    public CurrencyConvertResponse getConversionQuote(CurrencyConvertRequest request) {
        String from = request.fromCurrency().toUpperCase();
        String to = request.toCurrency().toUpperCase();

        if (!isValidCurrency(from) || !isValidCurrency(to)) {
            throw new IllegalArgumentException("Unsupported currency pair: " + from + " -> " + to);
        }

        BigDecimal rate = calculateCrossRate(from, to);
        BigDecimal inverseRate = calculateCrossRate(to, from);
        BigDecimal spread = new BigDecimal("0.0050"); // 0.5% conversion fee
        BigDecimal fee = request.amount().multiply(spread).setScale(4, RoundingMode.HALF_UP);
        BigDecimal netFromAmount = request.amount().subtract(fee);
        BigDecimal toAmount = netFromAmount.multiply(rate).setScale(4, RoundingMode.HALF_UP);

        return new CurrencyConvertResponse(
                from,
                to,
                request.amount().setScale(2, RoundingMode.HALF_UP),
                toAmount.setScale(2, RoundingMode.HALF_UP),
                rate.setScale(6, RoundingMode.HALF_UP),
                inverseRate.setScale(6, RoundingMode.HALF_UP),
                spread.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                fee.setScale(2, RoundingMode.HALF_UP),
                "QTE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                Instant.now()
        );
    }

    @Transactional
    public CurrencyTransferResponse executeTransfer(CurrencyTransferRequest request) {
        String from = request.fromCurrency().toUpperCase();
        String to = request.toCurrency().toUpperCase();

        if (!isValidCurrency(from) || !isValidCurrency(to)) {
            throw new IllegalArgumentException("Unsupported currency pair: " + from + " -> " + to);
        }

        if (from.equals(to)) {
            throw new IllegalArgumentException("Source and destination currencies cannot be identical");
        }

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        Wallet wallet = getOrCreateWallet(customer);

        WalletBalance fromBalance = walletBalanceRepository.findByWalletAndCurrency(wallet, from)
                .orElseThrow(() -> new IllegalStateException("Balance record not found for " + from));
        WalletBalance toBalance = walletBalanceRepository.findByWalletAndCurrency(wallet, to)
                .orElseThrow(() -> new IllegalStateException("Balance record not found for " + to));

        if (fromBalance.getAvailableAmount().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient funds in " + from + ". Available: " + fromBalance.getAvailableAmount() + ", Requested: " + request.amount());
        }

        BigDecimal rate = calculateCrossRate(from, to);
        BigDecimal spread = new BigDecimal("0.0050");
        BigDecimal fee = request.amount().multiply(spread).setScale(4, RoundingMode.HALF_UP);
        BigDecimal netFrom = request.amount().subtract(fee);
        BigDecimal toAmount = netFrom.multiply(rate).setScale(4, RoundingMode.HALF_UP);

        // Debit source balance
        fromBalance.setAvailableAmount(fromBalance.getAvailableAmount().subtract(request.amount()));
        fromBalance.setUpdatedAt(Instant.now());
        walletBalanceRepository.save(fromBalance);

        // Credit destination balance
        toBalance.setAvailableAmount(toBalance.getAvailableAmount().add(toAmount));
        toBalance.setUpdatedAt(Instant.now());
        walletBalanceRepository.save(toBalance);

        // Record transfer
        CurrencyTransfer transfer = new CurrencyTransfer();
        transfer.setWallet(wallet);
        transfer.setFromCurrency(from);
        transfer.setToCurrency(to);
        transfer.setFromAmount(request.amount());
        transfer.setToAmount(toAmount);
        transfer.setRateApplied(rate);
        transfer.setFeeAmount(fee);
        transfer.setStatus("COMPLETED");
        transfer.setReference(request.reference() != null && !request.reference().isBlank() ? request.reference() : "TXF_" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        transfer.setCreatedAt(Instant.now());

        CurrencyTransfer savedTransfer = currencyTransferRepository.save(transfer);

        auditService.logEvent(
                "CUSTOMER",
                customer.getCustomerId().toString(),
                "CURRENCY_EXCHANGE_EXECUTED",
                "CURRENCY_TRANSFER",
                savedTransfer.getTransferId().toString(),
                null,
                String.format("Exchanged %s %s -> %s %s at rate %s", request.amount(), from, toAmount, to, rate)
        );

        return toTransferResponse(savedTransfer);
    }

    public BigDecimal convertAmount(String fromCurrency, String toCurrency, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (fromCurrency.equalsIgnoreCase(toCurrency)) return amount;
        BigDecimal rate = calculateCrossRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCrossRate(String fromCurrency, String toCurrency) {
        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();

        if (from.equals(to)) return BigDecimal.ONE;

        // Base rate against GHS
        BigDecimal ghsToFrom = getRateFromGhs(from);
        BigDecimal ghsToTo = getRateFromGhs(to);

        // Rate from -> to = (GHS -> TO) / (GHS -> FROM)
        return ghsToTo.divide(ghsToFrom, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal getRateFromGhs(String currency) {
        if (currency.equals("GHS")) return BigDecimal.ONE;

        Optional<ExchangeRate> rateOpt = exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("GHS", currency);
        if (rateOpt.isPresent()) {
            return rateOpt.get().getRate();
        }

        // Standard consistent internal rates if not in database
        return switch (currency) {
            case "NGN" -> new BigDecimal("105.500000"); // 1 GHS = ~105.5 NGN
            case "KES" -> new BigDecimal("8.750000");   // 1 GHS = ~8.75 KES
            case "ZAR" -> new BigDecimal("1.180000");   // 1 GHS = ~1.18 ZAR
            case "EGP" -> new BigDecimal("3.250000");   // 1 GHS = ~3.25 EGP
            case "USD" -> new BigDecimal("0.065000");   // 1 GHS = ~0.065 USD ($1 = 15.38 GHS)
            case "GBP" -> new BigDecimal("0.051000");   // 1 GHS = ~0.051 GBP (£1 = 19.60 GHS)
            case "EUR" -> new BigDecimal("0.060000");   // 1 GHS = ~0.060 EUR (€1 = 16.66 GHS)
            default -> BigDecimal.ONE;
        };
    }

    private boolean isValidCurrency(String code) {
        if (code == null) return false;
        return SUPPORTED_CURRENCIES.stream().anyMatch(c -> c.code().equalsIgnoreCase(code));
    }

    private CurrencyTransferResponse toTransferResponse(CurrencyTransfer t) {
        return new CurrencyTransferResponse(
                t.getTransferId(),
                t.getWallet().getWalletId(),
                t.getFromCurrency(),
                t.getToCurrency(),
                t.getFromAmount().setScale(2, RoundingMode.HALF_UP),
                t.getToAmount().setScale(2, RoundingMode.HALF_UP),
                t.getRateApplied(),
                t.getFeeAmount().setScale(2, RoundingMode.HALF_UP),
                t.getStatus(),
                t.getReference(),
                t.getCreatedAt()
        );
    }
}
