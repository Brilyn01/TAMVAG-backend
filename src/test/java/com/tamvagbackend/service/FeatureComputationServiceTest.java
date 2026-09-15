package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.FeatureSnapshot;
import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.repository.CustomerRepository;
import com.tamvagbackend.domain.repository.FeatureSnapshotRepository;
import com.tamvagbackend.domain.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureComputationServiceTest {

    @Mock
    private FeatureSnapshotRepository featureSnapshotRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    private FeatureComputationService service;

    private UUID customerId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        service = new FeatureComputationService(
                featureSnapshotRepository,
                transactionRepository,
                customerRepository
        );

        customerId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerId(customerId);
    }

    @Test
    void computesCoreCashFlowFeatures() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        Transaction salary = transaction("IN", "2500.00",
                start.plusDays(4).atStartOfDay().toInstant(ZoneOffset.UTC));

        Transaction expense = transaction("OUT", "500.00",
                start.plusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC));

        Transaction expense2 = transaction("OUT", "250.00",
                start.plusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC));

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(transactionRepository.findByCustomerAndOccurredAtBetween(
                eq(customerId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(salary, expense, expense2));

        when(featureSnapshotRepository
                .findByCustomerAndPeriodStartAndPeriodEndAndFeatureVersion(
                        customer, start, end,
                        FeatureComputationService.FEATURE_VERSION))
                .thenReturn(Optional.empty());

        FeatureSnapshot savedSnapshot = new FeatureSnapshot();

        when(featureSnapshotRepository.save(any(FeatureSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeatureSnapshot result =
                service.compute(customerId, start, end);

        assertEquals(customer, result.getCustomer());
        assertEquals(start, result.getPeriodStart());
        assertEquals(end, result.getPeriodEnd());
        assertEquals(
                FeatureComputationService.FEATURE_VERSION,
                result.getFeatureVersion()
        );

        assertEquals(
                new BigDecimal("2500.0000"),
                result.getTotalInflows()
        );

        assertEquals(
                new BigDecimal("750.0000"),
                result.getTotalOutflows()
        );

        assertEquals(
                new BigDecimal("1750.0000"),
                result.getNetCashFlow()
        );

        assertEquals(3, result.getTransactionCount());
        assertEquals(1, result.getInflowTransactionCount());
        assertEquals(2, result.getOutflowTransactionCount());

        assertEquals(
                new BigDecimal("2500.0000"),
                result.getAverageInflow()
        );

        assertEquals(
                new BigDecimal("375.0000"),
                result.getAverageOutflow()
        );

        assertEquals(
                new BigDecimal("2500.0000"),
                result.getLargestInflow()
        );

        assertEquals(
                new BigDecimal("500.0000"),
                result.getLargestOutflow()
        );

        verify(featureSnapshotRepository).save(any(FeatureSnapshot.class));
    }

    @Test
    void supportsCustomerWithNoTransactions() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(transactionRepository.findByCustomerAndOccurredAtBetween(
                eq(customerId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        when(featureSnapshotRepository
                .findByCustomerAndPeriodStartAndPeriodEndAndFeatureVersion(
                        customer, start, end,
                        FeatureComputationService.FEATURE_VERSION))
                .thenReturn(Optional.empty());

        when(featureSnapshotRepository.save(any(FeatureSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeatureSnapshot result =
                service.compute(customerId, start, end);

        assertEquals(BigDecimal.ZERO.setScale(4), result.getTotalInflows());
        assertEquals(BigDecimal.ZERO.setScale(4), result.getTotalOutflows());
        assertEquals(BigDecimal.ZERO.setScale(4), result.getNetCashFlow());

        assertEquals(0, result.getTransactionCount());
        assertEquals(0, result.getInflowTransactionCount());
        assertEquals(0, result.getOutflowTransactionCount());

        assertEquals(BigDecimal.ZERO.setScale(4), result.getAverageInflow());
        assertEquals(BigDecimal.ZERO.setScale(4), result.getAverageOutflow());

        assertEquals(BigDecimal.ZERO.setScale(4), result.getLargestInflow());
        assertEquals(BigDecimal.ZERO.setScale(4), result.getLargestOutflow());

        assertEquals(BigDecimal.ZERO.setScale(8), result.getIncomeConsistency());
        assertEquals(BigDecimal.ZERO.setScale(8), result.getExpenseConsistency());
    }

    @Test
    void calculatesConsistencyForIdenticalAmountsAsOne() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        Transaction first = transaction("IN", "1000.00",
                start.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Transaction second = transaction("IN", "1000.00",
                start.plusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC));

        Transaction third = transaction("IN", "1000.00",
                start.plusDays(20).atStartOfDay().toInstant(ZoneOffset.UTC));

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(transactionRepository.findByCustomerAndOccurredAtBetween(
                eq(customerId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(first, second, third));

        when(featureSnapshotRepository
                .findByCustomerAndPeriodStartAndPeriodEndAndFeatureVersion(
                        customer, start, end,
                        FeatureComputationService.FEATURE_VERSION))
                .thenReturn(Optional.empty());

        when(featureSnapshotRepository.save(any(FeatureSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeatureSnapshot result =
                service.compute(customerId, start, end);

        assertEquals(
                new BigDecimal("1.00000000"),
                result.getIncomeConsistency()
        );
    }

    @Test
    void calculatesLowerConsistencyForVariableAmounts() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        Transaction first = transaction("IN", "500.00",
                start.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Transaction second = transaction("IN", "1500.00",
                start.plusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC));

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(transactionRepository.findByCustomerAndOccurredAtBetween(
                eq(customerId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(first, second));

        when(featureSnapshotRepository
                .findByCustomerAndPeriodStartAndPeriodEndAndFeatureVersion(
                        customer, start, end,
                        FeatureComputationService.FEATURE_VERSION))
                .thenReturn(Optional.empty());

        when(featureSnapshotRepository.save(any(FeatureSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeatureSnapshot result =
                service.compute(customerId, start, end);

        assertTrue(
                result.getIncomeConsistency()
                        .compareTo(BigDecimal.ONE) < 0
        );

        assertTrue(
                result.getIncomeConsistency()
                        .compareTo(BigDecimal.ZERO) > 0
        );
    }

    @Test
    void reusesExistingSnapshotForSameCustomerPeriodAndVersion() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        FeatureSnapshot existing = new FeatureSnapshot();
        existing.setCustomer(customer);
        existing.setPeriodStart(start);
        existing.setPeriodEnd(end);
        existing.setFeatureVersion(
                FeatureComputationService.FEATURE_VERSION
        );

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(transactionRepository.findByCustomerAndOccurredAtBetween(
                eq(customerId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        when(featureSnapshotRepository
                .findByCustomerAndPeriodStartAndPeriodEndAndFeatureVersion(
                        customer, start, end,
                        FeatureComputationService.FEATURE_VERSION))
                .thenReturn(Optional.of(existing));

        when(featureSnapshotRepository.save(any(FeatureSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeatureSnapshot result =
                service.compute(customerId, start, end);

        assertSame(existing, result);

        verify(featureSnapshotRepository).save(existing);
    }

    @Test
    void includesStartBoundaryAndExcludesNextPeriod() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(transactionRepository.findByCustomerAndOccurredAtBetween(
                eq(customerId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        when(featureSnapshotRepository
                .findByCustomerAndPeriodStartAndPeriodEndAndFeatureVersion(
                        customer, start, end,
                        FeatureComputationService.FEATURE_VERSION))
                .thenReturn(Optional.empty());

        when(featureSnapshotRepository.save(any(FeatureSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.compute(customerId, start, end);

        ArgumentCaptor<Instant> startCaptor =
                ArgumentCaptor.forClass(Instant.class);

        ArgumentCaptor<Instant> endCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(transactionRepository).findByCustomerAndOccurredAtBetween(
                eq(customerId),
                startCaptor.capture(),
                endCaptor.capture()
        );

        assertEquals(
                start.atStartOfDay().toInstant(ZoneOffset.UTC),
                startCaptor.getValue()
        );

        assertEquals(
                end.plusDays(1)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC),
                endCaptor.getValue()
        );
    }

    @Test
    void rejectsMissingCustomerId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.compute(
                        null,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )
        );

        verifyNoInteractions(
                customerRepository,
                transactionRepository,
                featureSnapshotRepository
        );
    }

    @Test
    void rejectsMissingPeriodStart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.compute(
                        customerId,
                        null,
                        LocalDate.of(2026, 9, 30)
                )
        );
    }

    @Test
    void rejectsMissingPeriodEnd() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.compute(
                        customerId,
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
    }

    @Test
    void rejectsReversedPeriod() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.compute(
                        customerId,
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 9, 1)
                )
        );
    }

    @Test
    void rejectsUnknownCustomer() {
        when(customerRepository.findById(customerId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.compute(
                        customerId,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )
        );

        verifyNoInteractions(
                transactionRepository,
                featureSnapshotRepository
        );
    }

    private Transaction transaction(
            String direction,
            String amount,
            Instant occurredAt) {

        Account account = new Account();
        account.setCustomer(customer);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setAccount(account);
        transaction.setDirection(direction);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setOccurredAt(occurredAt);
        transaction.setStatus("COMPLETED");
        transaction.setCurrency("GHS");

        return transaction;
    }
}