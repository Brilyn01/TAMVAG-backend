package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.FeatureSnapshot;
import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.domain.repository.CustomerRepository;
import com.tamvagbackend.domain.repository.FeatureSnapshotRepository;
import com.tamvagbackend.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class FeatureComputationService {

    public static final String FEATURE_VERSION = "1.0.0";

    private final FeatureSnapshotRepository featureSnapshotRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    public FeatureComputationService(
            FeatureSnapshotRepository featureSnapshotRepository,
            TransactionRepository transactionRepository,
            CustomerRepository customerRepository) {
        this.featureSnapshotRepository = featureSnapshotRepository;
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public FeatureSnapshot compute(
            UUID customerId,
            LocalDate periodStart,
            LocalDate periodEnd) {

        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }

        if (periodStart == null) {
            throw new IllegalArgumentException("Period start is required");
        }

        if (periodEnd == null) {
            throw new IllegalArgumentException("Period end is required");
        }

        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException(
                    "Period end cannot be before period start");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found: " + customerId));

        Instant start = periodStart
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        Instant endExclusive = periodEnd
                .plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        List<Transaction> transactions =
                transactionRepository.findByCustomerAndOccurredAtBetween(
                        customerId,
                        start,
                        endExclusive
                );

        FeatureSnapshot snapshot =
                featureSnapshotRepository
                        .findByCustomerAndPeriodStartAndPeriodEndAndFeatureVersion(
                                customer,
                                periodStart,
                                periodEnd,
                                FEATURE_VERSION
                        )
                        .orElseGet(FeatureSnapshot::new);

        snapshot.setCustomer(customer);
        snapshot.setPeriodStart(periodStart);
        snapshot.setPeriodEnd(periodEnd);
        snapshot.setFeatureVersion(FEATURE_VERSION);

        BigDecimal totalInflows = BigDecimal.ZERO;
        BigDecimal totalOutflows = BigDecimal.ZERO;
        BigDecimal largestInflow = BigDecimal.ZERO;
        BigDecimal largestOutflow = BigDecimal.ZERO;

        long inflowCount = 0;
        long outflowCount = 0;

        for (Transaction transaction : transactions) {

            if (transaction == null || transaction.getAmount() == null) {
                continue;
            }

            BigDecimal amount = transaction.getAmount().abs();

            if ("IN".equalsIgnoreCase(transaction.getDirection())) {
                totalInflows = totalInflows.add(amount);
                inflowCount++;
                largestInflow = largestInflow.max(amount);

            } else if ("OUT".equalsIgnoreCase(transaction.getDirection())) {
                totalOutflows = totalOutflows.add(amount);
                outflowCount++;
                largestOutflow = largestOutflow.max(amount);
            }
        }

        long transactionCount = inflowCount + outflowCount;

        BigDecimal averageInflow =
                inflowCount == 0
                        ? zeroScale4()
                        : totalInflows.divide(
                                BigDecimal.valueOf(inflowCount),
                                4,
                                RoundingMode.HALF_UP
                        );

        BigDecimal averageOutflow =
                outflowCount == 0
                        ? zeroScale4()
                        : totalOutflows.divide(
                                BigDecimal.valueOf(outflowCount),
                                4,
                                RoundingMode.HALF_UP
                        );

        BigDecimal netCashFlow =
                totalInflows.subtract(totalOutflows);

        BigDecimal incomeConsistency =
                calculateConsistency(transactions, "IN");

        BigDecimal expenseConsistency =
                calculateConsistency(transactions, "OUT");

        snapshot.setTotalInflows(scale4(totalInflows));
        snapshot.setTotalOutflows(scale4(totalOutflows));
        snapshot.setNetCashFlow(scale4(netCashFlow));
        snapshot.setTransactionCount(transactionCount);
        snapshot.setInflowTransactionCount(inflowCount);
        snapshot.setOutflowTransactionCount(outflowCount);
        snapshot.setAverageInflow(averageInflow);
        snapshot.setAverageOutflow(averageOutflow);
        snapshot.setLargestInflow(scale4(largestInflow));
        snapshot.setLargestOutflow(scale4(largestOutflow));
        snapshot.setIncomeConsistency(incomeConsistency);
        snapshot.setExpenseConsistency(expenseConsistency);
        snapshot.setComputedAt(Instant.now());

        return featureSnapshotRepository.save(snapshot);
    }

    private BigDecimal calculateConsistency(
            List<Transaction> transactions,
            String direction) {

        List<BigDecimal> amounts = transactions.stream()
                .filter(transaction ->
                        transaction != null
                                && transaction.getAmount() != null
                                && direction.equalsIgnoreCase(
                                transaction.getDirection()))
                .map(transaction ->
                        transaction.getAmount().abs())
                .toList();

        if (amounts.isEmpty()) {
            return zeroScale8();
        }

        BigDecimal mean = amounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                        BigDecimal.valueOf(amounts.size()),
                        8,
                        RoundingMode.HALF_UP
                );

        if (mean.compareTo(BigDecimal.ZERO) == 0) {
            return zeroScale8();
        }

        BigDecimal variance = amounts.stream()
                .map(amount ->
                        amount.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                        BigDecimal.valueOf(amounts.size()),
                        8,
                        RoundingMode.HALF_UP
                );

        double standardDeviation =
                Math.sqrt(variance.doubleValue());

        double coefficientOfVariation =
                standardDeviation / mean.doubleValue();

        double consistency =
                1.0 / (1.0 + coefficientOfVariation);

        return BigDecimal.valueOf(consistency)
                .setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal scale4(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroScale4() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroScale8() {
        return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
    }
}