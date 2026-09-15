package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "feature_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_feature_snapshot_customer_period_version",
                        columnNames = {
                                "customer_id",
                                "period_start",
                                "period_end",
                                "feature_version"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_feature_snapshot_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_feature_snapshot_period",
                        columnList = "period_start, period_end"
                )
        }
)
public class FeatureSnapshot {

    @Id
    @Column(name = "feature_snapshot_id", nullable = false)
    private UUID featureSnapshotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "feature_version", nullable = false, length = 50)
    private String featureVersion;

    @Column(name = "total_inflows", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalInflows = BigDecimal.ZERO;

    @Column(name = "total_outflows", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalOutflows = BigDecimal.ZERO;

    @Column(name = "net_cash_flow", nullable = false, precision = 20, scale = 4)
    private BigDecimal netCashFlow = BigDecimal.ZERO;

    @Column(name = "transaction_count", nullable = false)
    private long transactionCount;

    @Column(name = "inflow_transaction_count", nullable = false)
    private long inflowTransactionCount;

    @Column(name = "outflow_transaction_count", nullable = false)
    private long outflowTransactionCount;

    @Column(name = "average_inflow", nullable = false, precision = 20, scale = 4)
    private BigDecimal averageInflow = BigDecimal.ZERO;

    @Column(name = "average_outflow", nullable = false, precision = 20, scale = 4)
    private BigDecimal averageOutflow = BigDecimal.ZERO;

    @Column(name = "largest_inflow", nullable = false, precision = 20, scale = 4)
    private BigDecimal largestInflow = BigDecimal.ZERO;

    @Column(name = "largest_outflow", nullable = false, precision = 20, scale = 4)
    private BigDecimal largestOutflow = BigDecimal.ZERO;

    @Column(name = "income_consistency", nullable = false, precision = 20, scale = 8)
    private BigDecimal incomeConsistency = BigDecimal.ZERO;

    @Column(name = "expense_consistency", nullable = false, precision = 20, scale = 8)
    private BigDecimal expenseConsistency = BigDecimal.ZERO;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    public FeatureSnapshot() {
        this.featureSnapshotId = UUID.randomUUID();
        this.computedAt = Instant.now();
    }

    public UUID getFeatureSnapshotId() {
        return featureSnapshotId;
    }

    public void setFeatureSnapshotId(UUID featureSnapshotId) {
        this.featureSnapshotId = featureSnapshotId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getFeatureVersion() {
        return featureVersion;
    }

    public void setFeatureVersion(String featureVersion) {
        this.featureVersion = featureVersion;
    }

    public BigDecimal getTotalInflows() {
        return totalInflows;
    }

    public void setTotalInflows(BigDecimal totalInflows) {
        this.totalInflows = totalInflows;
    }

    public BigDecimal getTotalOutflows() {
        return totalOutflows;
    }

    public void setTotalOutflows(BigDecimal totalOutflows) {
        this.totalOutflows = totalOutflows;
    }

    public BigDecimal getNetCashFlow() {
        return netCashFlow;
    }

    public void setNetCashFlow(BigDecimal netCashFlow) {
        this.netCashFlow = netCashFlow;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public long getInflowTransactionCount() {
        return inflowTransactionCount;
    }

    public void setInflowTransactionCount(long inflowTransactionCount) {
        this.inflowTransactionCount = inflowTransactionCount;
    }

    public long getOutflowTransactionCount() {
        return outflowTransactionCount;
    }

    public void setOutflowTransactionCount(long outflowTransactionCount) {
        this.outflowTransactionCount = outflowTransactionCount;
    }

    public BigDecimal getAverageInflow() {
        return averageInflow;
    }

    public void setAverageInflow(BigDecimal averageInflow) {
        this.averageInflow = averageInflow;
    }

    public BigDecimal getAverageOutflow() {
        return averageOutflow;
    }

    public void setAverageOutflow(BigDecimal averageOutflow) {
        this.averageOutflow = averageOutflow;
    }

    public BigDecimal getLargestInflow() {
        return largestInflow;
    }

    public void setLargestInflow(BigDecimal largestInflow) {
        this.largestInflow = largestInflow;
    }

    public BigDecimal getLargestOutflow() {
        return largestOutflow;
    }

    public void setLargestOutflow(BigDecimal largestOutflow) {
        this.largestOutflow = largestOutflow;
    }

    public BigDecimal getIncomeConsistency() {
        return incomeConsistency;
    }

    public void setIncomeConsistency(BigDecimal incomeConsistency) {
        this.incomeConsistency = incomeConsistency;
    }

    public BigDecimal getExpenseConsistency() {
        return expenseConsistency;
    }

    public void setExpenseConsistency(BigDecimal expenseConsistency) {
        this.expenseConsistency = expenseConsistency;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }
}