package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.*;
import com.tamvagbackend.domain.repository.*;
import com.tamvagbackend.dto.ProfileDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ProfileService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public ProfileService(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository
    ) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getCustomerProfile(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        List<Account> accounts = accountRepository.findByCustomer(customer);
        List<Transaction> allTxs = transactionRepository.findAllByCustomerId(customerId);

        Instant now = Instant.now();
        Instant ninetyDaysAgo = now.minus(Duration.ofDays(90));
        Instant thirtyDaysAgo = now.minus(Duration.ofDays(30));

        BigDecimal totalInflows90d = BigDecimal.ZERO;
        BigDecimal totalOutflows90d = BigDecimal.ZERO;
        BigDecimal totalSavings90d = BigDecimal.ZERO;
        BigDecimal totalDebtRepayment30d = BigDecimal.ZERO;
        BigDecimal totalInflows30d = BigDecimal.ZERO;

        int inboundTxCount = 0;

        for (Transaction tx : allTxs) {
            BigDecimal amt = tx.getAmount();
            if (tx.getOccurredAt().isAfter(ninetyDaysAgo)) {
                if ("IN".equalsIgnoreCase(tx.getDirection())) {
                    totalInflows90d = totalInflows90d.add(amt);
                    inboundTxCount++;
                } else {
                    totalOutflows90d = totalOutflows90d.add(amt);
                    if (tx.getReference() != null && tx.getReference().toLowerCase().contains("save")) {
                        totalSavings90d = totalSavings90d.add(amt);
                    }
                }
            }

            if (tx.getOccurredAt().isAfter(thirtyDaysAgo)) {
                if ("IN".equalsIgnoreCase(tx.getDirection())) {
                    totalInflows30d = totalInflows30d.add(amt);
                } else if (tx.getReference() != null && tx.getReference().toLowerCase().contains("loan")) {
                    totalDebtRepayment30d = totalDebtRepayment30d.add(amt);
                }
            }
        }

        // Monthly estimate (90-day total / 3)
        BigDecimal monthlyIncomeEst = totalInflows90d.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        BigDecimal net90d = totalInflows90d.subtract(totalOutflows90d);

        // Ratios
        BigDecimal savingsRate90d = totalInflows90d.compareTo(BigDecimal.ZERO) > 0
                ? totalSavings90d.divide(totalInflows90d, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal debtServiceRatio30d = totalInflows30d.compareTo(BigDecimal.ZERO) > 0
                ? totalDebtRepayment30d.divide(totalInflows30d, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String debtPressure = debtServiceRatio30d.compareTo(new BigDecimal("0.40")) > 0 ? "HIGH"
                : (debtServiceRatio30d.compareTo(new BigDecimal("0.20")) > 0 ? "MODERATE" : "LOW");

        // Income consistency
        BigDecimal consistency = inboundTxCount >= 6 ? new BigDecimal("0.88") : (inboundTxCount >= 2 ? new BigDecimal("0.72") : new BigDecimal("0.50"));
        BigDecimal confidence = allTxs.size() >= 10 ? new BigDecimal("0.92") : new BigDecimal("0.70");

        int confidenceScore = (int) (confidence.doubleValue() * 100);

        return new CustomerProfileResponse(
                customerId,
                now,
                new IncomeSummary(monthlyIncomeEst, consistency, confidence),
                new CashFlowSummary(
                        totalInflows90d.setScale(2, RoundingMode.HALF_UP),
                        totalOutflows90d.setScale(2, RoundingMode.HALF_UP),
                        net90d.setScale(2, RoundingMode.HALF_UP),
                        new BigDecimal("0.18")
                ),
                new SavingsSummary(
                        new BigDecimal("0.75"),
                        savingsRate90d,
                        totalSavings90d.multiply(new BigDecimal("1.5")).setScale(2, RoundingMode.HALF_UP)
                ),
                new DebtSummary(
                        debtPressure,
                        debtServiceRatio30d,
                        new BigDecimal("0.90")
                ),
                confidenceScore,
                365,
                "v1.0",
                accounts.size(),
                allTxs.size()
        );
    }
}
