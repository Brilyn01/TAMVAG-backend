package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.domain.repository.AccountRepository;
import com.tamvagbackend.domain.repository.CustomerRepository;
import com.tamvagbackend.domain.repository.TransactionRepository;
import com.tamvagbackend.dto.ProfileDtos.CashFlowSummary;
import com.tamvagbackend.dto.ProfileDtos.CustomerProfileResponse;
import com.tamvagbackend.dto.ProfileDtos.DebtSummary;
import com.tamvagbackend.dto.ProfileDtos.IncomeSummary;
import com.tamvagbackend.dto.ProfileDtos.SavingsSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ProfileService {

    private static final int PROFILE_EVIDENCE_WINDOW_DAYS = 90;
    private static final int DEBT_EVIDENCE_WINDOW_DAYS = 30;

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE =
            BigDecimal.ONE;

    private static final BigDecimal TWENTY_PERCENT =
            new BigDecimal("0.20");

    private static final BigDecimal FORTY_PERCENT =
            new BigDecimal("0.40");

    private static final BigDecimal HIGH_CONFIDENCE =
            new BigDecimal("0.90");

    private static final BigDecimal MEDIUM_CONFIDENCE =
            new BigDecimal("0.70");

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
    public CustomerProfileResponse getCustomerProfile(
            UUID customerId
    ) {
        if (customerId == null) {
            throw new IllegalArgumentException(
                    "customer_id is required"
            );
        }

        Customer customer =
                customerRepository.findById(customerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Customer not found: "
                                                + customerId
                                )
                        );

        List<Account> accounts =
                accountRepository.findByCustomer(customer);

        List<Transaction> allTransactions =
                transactionRepository
                        .findAllByCustomerId(customerId);

        Instant now = Instant.now();

        Instant ninetyDaysAgo =
                now.minus(
                        Duration.ofDays(
                                PROFILE_EVIDENCE_WINDOW_DAYS
                        )
                );

        Instant thirtyDaysAgo =
                now.minus(
                        Duration.ofDays(
                                DEBT_EVIDENCE_WINDOW_DAYS
                        )
                );

        List<Transaction> transactions90d =
                filterSince(
                        allTransactions,
                        ninetyDaysAgo
                );

        List<Transaction> transactions30d =
                filterSince(
                        allTransactions,
                        thirtyDaysAgo
                );

        BigDecimal totalInflows90d =
                sumDirection(
                        transactions90d,
                        "IN"
                );

        BigDecimal totalOutflows90d =
                sumDirection(
                        transactions90d,
                        "OUT"
                );

        BigDecimal totalSavings90d =
                sumSavings(
                        transactions90d
                );

        BigDecimal totalDebtRepayment30d =
                sumLoanOutflows(
                        transactions30d
                );

        BigDecimal totalInflows30d =
                sumDirection(
                        transactions30d,
                        "IN"
                );

        int inboundTxCount =
                countDirection(
                        transactions90d,
                        "IN"
                );

        int savingsTransactionCount =
                countSavingsTransactions(
                        transactions90d
                );

        int repaymentTransactionCount =
                countLoanOutflows(
                        transactions30d
                );

        BigDecimal monthlyIncomeEstimate =
                totalInflows90d
                        .divide(
                                new BigDecimal("3"),
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal net90d =
                totalInflows90d
                        .subtract(totalOutflows90d);

        BigDecimal savingsRate90d =
                divideSafely(
                        totalSavings90d,
                        totalInflows90d
                );

        BigDecimal debtServiceRatio30d =
                divideSafely(
                        totalDebtRepayment30d,
                        totalInflows30d
                );

        String debtPressure =
                calculateDebtPressure(
                        debtServiceRatio30d
                );

        BigDecimal incomeConsistency =
                calculateIncomeConsistency(
                        transactions90d
                );

        BigDecimal incomeConfidence =
                calculateIncomeConfidence(
                        transactions90d,
                        accounts
                );

        BigDecimal cashFlowVolatility =
                calculateCashFlowVolatility(
                        transactions90d
                );

        BigDecimal savingsConsistency =
                calculateSavingsConsistency(
                        savingsTransactionCount,
                        transactions90d
                );

        BigDecimal repaymentConsistency =
                calculateRepaymentConsistency(
                        repaymentTransactionCount,
                        transactions30d
                );

        int confidenceScore =
                calculateConfidenceScore(
                        transactions90d,
                        accounts
                );

        /*
         * The available transaction model provides observed
         * savings flows, not a separate authoritative savings
         * account balance. Therefore we expose cumulative
         * observed savings over the evidence window instead of
         * applying an artificial multiplier.
         */
        BigDecimal estimatedSavingsBalance =
                totalSavings90d.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        return new CustomerProfileResponse(
                customerId,
                now,
                new IncomeSummary(
                        monthlyIncomeEstimate,
                        incomeConsistency,
                        incomeConfidence
                ),
                new CashFlowSummary(
                        scale2(totalInflows90d),
                        scale2(totalOutflows90d),
                        scale2(net90d),
                        cashFlowVolatility
                ),
                new SavingsSummary(
                        savingsConsistency,
                        savingsRate90d,
                        estimatedSavingsBalance
                ),
                new DebtSummary(
                        debtPressure,
                        debtServiceRatio30d,
                        repaymentConsistency
                ),
                confidenceScore,
                PROFILE_EVIDENCE_WINDOW_DAYS,
                "v1.1",
                accounts.size(),
                transactions90d.size()
        );
    }

    private List<Transaction> filterSince(
            List<Transaction> transactions,
            Instant since
    ) {
        if (transactions == null
                || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Transaction> result =
                new ArrayList<>();

        for (Transaction transaction : transactions) {
            if (transaction == null
                    || transaction.getOccurredAt() == null) {
                continue;
            }

            if (!transaction.getOccurredAt()
                    .isBefore(since)) {
                result.add(transaction);
            }
        }

        return result;
    }

    private BigDecimal sumDirection(
            List<Transaction> transactions,
            String direction
    ) {
        BigDecimal total = ZERO;

        for (Transaction transaction : transactions) {
            if (transaction.getAmount() == null) {
                continue;
            }

            if (direction.equalsIgnoreCase(
                    transaction.getDirection()
            )) {
                total = total.add(
                        transaction.getAmount().abs()
                );
            }
        }

        return total;
    }

    private int countDirection(
            List<Transaction> transactions,
            String direction
    ) {
        int count = 0;

        for (Transaction transaction : transactions) {
            if (direction.equalsIgnoreCase(
                    transaction.getDirection()
            )) {
                count++;
            }
        }

        return count;
    }

    private BigDecimal sumSavings(
            List<Transaction> transactions
    ) {
        BigDecimal total = ZERO;

        for (Transaction transaction : transactions) {
            if (!isOutflow(transaction)
                    || transaction.getAmount() == null) {
                continue;
            }

            if (isSavingsTransaction(transaction)) {
                total = total.add(
                        transaction.getAmount().abs()
                );
            }
        }

        return total;
    }

    private int countSavingsTransactions(
            List<Transaction> transactions
    ) {
        int count = 0;

        for (Transaction transaction : transactions) {
            if (isOutflow(transaction)
                    && isSavingsTransaction(transaction)) {
                count++;
            }
        }

        return count;
    }

    private BigDecimal sumLoanOutflows(
            List<Transaction> transactions
    ) {
        BigDecimal total = ZERO;

        for (Transaction transaction : transactions) {
            if (!isOutflow(transaction)
                    || transaction.getAmount() == null) {
                continue;
            }

            if (isLoanTransaction(transaction)) {
                total = total.add(
                        transaction.getAmount().abs()
                );
            }
        }

        return total;
    }

    private int countLoanOutflows(
            List<Transaction> transactions
    ) {
        int count = 0;

        for (Transaction transaction : transactions) {
            if (isOutflow(transaction)
                    && isLoanTransaction(transaction)) {
                count++;
            }
        }

        return count;
    }

    private boolean isOutflow(
            Transaction transaction
    ) {
        return transaction != null
                && "OUT".equalsIgnoreCase(
                        transaction.getDirection()
                );
    }

    private boolean isSavingsTransaction(
            Transaction transaction
    ) {
        String reference =
                transaction.getReference();

        return reference != null
                && reference
                .toLowerCase()
                .contains("save");
    }

    private boolean isLoanTransaction(
            Transaction transaction
    ) {
        String reference =
                transaction.getReference();

        return reference != null
                && reference
                .toLowerCase()
                .contains("loan");
    }

    private BigDecimal calculateIncomeConsistency(
            List<Transaction> transactions
    ) {
        if (transactions.isEmpty()) {
            return ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        int inboundCount =
                countDirection(
                        transactions,
                        "IN"
                );

        /*
         * Six or more inbound observations over 90 days
         * indicates the strongest available consistency
         * band in this transaction-based profile.
         */
        if (inboundCount >= 6) {
            return new BigDecimal("0.88");
        }

        if (inboundCount >= 2) {
            return new BigDecimal("0.72");
        }

        return new BigDecimal("0.50");
    }

    private BigDecimal calculateIncomeConfidence(
            List<Transaction> transactions,
            List<Account> accounts
    ) {
        if (transactions.size() >= 20
                && !accounts.isEmpty()) {
            return HIGH_CONFIDENCE;
        }

        if (transactions.size() >= 10
                || !accounts.isEmpty()) {
            return MEDIUM_CONFIDENCE;
        }

        if (!transactions.isEmpty()) {
            return new BigDecimal("0.50");
        }

        return ZERO;
    }

    private BigDecimal calculateCashFlowVolatility(
            List<Transaction> transactions
    ) {
        if (transactions.size() < 2) {
            return ZERO.setScale(
                    4,
                    RoundingMode.HALF_UP
            );
        }

        /*
         * Calculate population standard deviation of absolute
         * transaction amounts and normalize it by the mean.
         *
         * This produces a coefficient-of-variation style
         * measure:
         *
         *   standard deviation / mean
         *
         * A value of 0 means all observed transaction amounts
         * are identical.
         */
        BigDecimal sum = ZERO;
        int count = 0;

        for (Transaction transaction : transactions) {
            if (transaction.getAmount() == null) {
                continue;
            }

            sum = sum.add(
                    transaction.getAmount().abs()
            );
            count++;
        }

        if (count < 2
                || sum.compareTo(ZERO) == 0) {
            return ZERO.setScale(
                    4,
                    RoundingMode.HALF_UP
            );
        }

        BigDecimal mean =
                sum.divide(
                        BigDecimal.valueOf(count),
                        8,
                        RoundingMode.HALF_UP
                );

        BigDecimal squaredDifferenceSum =
                ZERO;

        for (Transaction transaction : transactions) {
            if (transaction.getAmount() == null) {
                continue;
            }

            BigDecimal difference =
                    transaction.getAmount()
                            .abs()
                            .subtract(mean);

            squaredDifferenceSum =
                    squaredDifferenceSum.add(
                            difference.multiply(difference)
                    );
        }

        BigDecimal variance =
                squaredDifferenceSum
                        .divide(
                                BigDecimal.valueOf(count),
                                8,
                                RoundingMode.HALF_UP
                        );

        double standardDeviation =
                Math.sqrt(
                        variance.doubleValue()
                );

        BigDecimal volatility =
                BigDecimal.valueOf(
                        standardDeviation
                ).divide(
                        mean,
                        4,
                        RoundingMode.HALF_UP
                );

        return volatility;
    }

    private BigDecimal calculateSavingsConsistency(
            int savingsTransactionCount,
            List<Transaction> transactions
    ) {
        if (transactions.isEmpty()) {
            return ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        /*
         * Savings consistency is the observed share of
         * transactions that are identified as savings flows.
         *
         * This is intentionally derived from the available
         * transaction evidence rather than hard-coded.
         */
        BigDecimal ratio =
                BigDecimal.valueOf(
                        savingsTransactionCount
                ).divide(
                        BigDecimal.valueOf(
                                transactions.size()
                        ),
                        4,
                        RoundingMode.HALF_UP
                );

        return ratio.min(ONE);
    }

    private BigDecimal calculateRepaymentConsistency(
            int repaymentTransactionCount,
            List<Transaction> transactions
    ) {
        if (transactions.isEmpty()) {
            return ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        /*
         * Repayment consistency is based on the observed
         * frequency of loan-related outflows in the 30-day
         * evidence window.
         */
        BigDecimal ratio =
                BigDecimal.valueOf(
                        repaymentTransactionCount
                ).divide(
                        BigDecimal.valueOf(
                                transactions.size()
                        ),
                        4,
                        RoundingMode.HALF_UP
                );

        return ratio.min(ONE);
    }

    private int calculateConfidenceScore(
            List<Transaction> transactions,
            List<Account> accounts
    ) {
        int transactionScore;

        if (transactions.size() >= 30) {
            transactionScore = 100;
        } else if (transactions.size() >= 20) {
            transactionScore = 90;
        } else if (transactions.size() >= 10) {
            transactionScore = 75;
        } else if (transactions.size() >= 5) {
            transactionScore = 60;
        } else if (!transactions.isEmpty()) {
            transactionScore = 40;
        } else {
            transactionScore = 0;
        }

        int accountScore;

        if (accounts.size() >= 3) {
            accountScore = 100;
        } else if (accounts.size() == 2) {
            accountScore = 85;
        } else if (accounts.size() == 1) {
            accountScore = 70;
        } else {
            accountScore = 0;
        }

        /*
         * Transaction evidence is weighted more heavily because
         * the profile itself is transaction-derived.
         */
        return (int) Math.round(
                transactionScore * 0.75
                        + accountScore * 0.25
        );
    }

    private String calculateDebtPressure(
            BigDecimal debtServiceRatio
    ) {
        if (debtServiceRatio.compareTo(
                FORTY_PERCENT
        ) > 0) {
            return "HIGH";
        }

        if (debtServiceRatio.compareTo(
                TWENTY_PERCENT
        ) > 0) {
            return "MODERATE";
        }

        return "LOW";
    }

    private BigDecimal divideSafely(
            BigDecimal numerator,
            BigDecimal denominator
    ) {
        if (numerator == null
                || denominator == null
                || denominator.compareTo(ZERO) <= 0) {
            return ZERO.setScale(
                    4,
                    RoundingMode.HALF_UP
            );
        }

        return numerator.divide(
                denominator,
                4,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal scale2(
            BigDecimal value
    ) {
        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
}