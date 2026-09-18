package com.tamvagbackend.service;

import com.tamvagbackend.config.RiskRulesProperties;
import com.tamvagbackend.domain.entity.Beneficiary;
import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Device;
import com.tamvagbackend.domain.entity.Transaction;
import com.tamvagbackend.domain.repository.BeneficiaryRepository;
import com.tamvagbackend.domain.repository.DeviceRepository;
import com.tamvagbackend.domain.repository.TransactionRepository;
import com.tamvagbackend.dto.RiskDtos.ReasonDetail;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ConfigurableRiskRulesEngine {

    private final DeviceRepository deviceRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransactionRepository transactionRepository;
    private final RiskRulesProperties properties;

    public ConfigurableRiskRulesEngine(
            DeviceRepository deviceRepository,
            BeneficiaryRepository beneficiaryRepository,
            TransactionRepository transactionRepository,
            RiskRulesProperties properties
    ) {
        this.deviceRepository = deviceRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.transactionRepository = transactionRepository;
        this.properties = properties;
    }

    public RuleEvaluation evaluate(
            Customer customer,
            String deviceId,
            String beneficiaryId,
            java.math.BigDecimal amount,
            Map<String, Object> context,
            Instant now
    ) {
        List<ReasonDetail> reasons = new ArrayList<>();
        List<String> reasonCodes = new ArrayList<>();
        int riskScore = 0;

        RiskRulesProperties.Rule newDevice = properties.getNewDevice();

        if (deviceId != null && !deviceId.isBlank()) {
            Optional<Device> device =
                    deviceRepository.findByCustomerAndFingerprintToken(customer, deviceId);

            Instant threshold = now.minus(
                    Duration.ofMillis((long) (newDevice.getWindowHours() * 3_600_000))
            );

            if (device.isEmpty() || device.get().getFirstSeenAt().isAfter(threshold)) {
                reasons.add(new ReasonDetail(
                        newDevice.getCode(),
                        "New or unrecognised device used within configured window",
                        "MEDIUM"
                ));
                reasonCodes.add("NEW_DEVICE");
                riskScore += newDevice.getScore();

                if (device.isEmpty()) {
                    deviceRepository.save(new Device(customer, deviceId));
                }
            }
        }

        RiskRulesProperties.Rule newBeneficiary =
                properties.getNewBeneficiary();

        if (beneficiaryId != null && !beneficiaryId.isBlank()) {
            Optional<Beneficiary> beneficiary =
                    beneficiaryRepository.findByCustomerAndToken(customer, beneficiaryId);

            Instant threshold = now.minus(
                    Duration.ofMillis((long) (newBeneficiary.getWindowHours() * 3_600_000))
            );

            if (beneficiary.isEmpty()
                    || beneficiary.get().getFirstSeenAt().isAfter(threshold)) {

                reasons.add(new ReasonDetail(
                        newBeneficiary.getCode(),
                        "First-time transfer destination within configured window",
                        "MEDIUM"
                ));
                reasonCodes.add("NEW_BENEFICIARY");
                riskScore += newBeneficiary.getScore();

                if (beneficiary.isEmpty()) {
                    beneficiaryRepository.save(new Beneficiary(customer, beneficiaryId));
                }
            }
        }

        List<Transaction> recentTransactions =
                transactionRepository.findRecentByCustomer(
                        customer.getCustomerId(),
                        now.minus(Duration.ofDays(90))
                );

        RiskRulesProperties.UnusualAmount unusualAmount =
                properties.getUnusualAmount();

        if (!recentTransactions.isEmpty()) {
            double average = recentTransactions.stream()
                    .mapToDouble(transaction -> transaction.getAmount().doubleValue())
                    .average()
                    .orElse(500.0);

            if (amount.doubleValue()
                    > average * unusualAmount.getMultiplier()
                    && amount.doubleValue() > unusualAmount.getMinimumAmount()) {

                reasons.add(new ReasonDetail(
                        unusualAmount.getCode(),
                        "Transaction amount materially exceeds customer historical baseline",
                        "HIGH"
                ));
                reasonCodes.add("UNUSUAL_AMOUNT");
                riskScore += unusualAmount.getScore();

            }
        } else if (amount.doubleValue()
                > unusualAmount.getNewCustomerThreshold()) {

            reasons.add(new ReasonDetail(
                    unusualAmount.getCode(),
                    "Large transaction amount for new customer profile",
                    "HIGH"
            ));
            reasonCodes.add("UNUSUAL_AMOUNT");
            riskScore += unusualAmount.getNewCustomerScore();
        }

        RiskRulesProperties.Velocity velocity = properties.getVelocity();

        long outboundCount = recentTransactions.stream()
                .filter(t -> t.getOccurredAt().isAfter(
                        now.minus(Duration.ofMillis(
                                (long) (velocity.getWindowHours() * 3_600_000)
                        ))
                ))
                .filter(t -> "OUT".equalsIgnoreCase(t.getDirection()))
                .count();

        if (outboundCount >= velocity.getTransactionCountThreshold()) {
            reasons.add(new ReasonDetail(
                    velocity.getCode(),
                    "Rapid repeated outbound transactions within configured window",
                    "HIGH"
            ));
            reasonCodes.add("VELOCITY_SPIKE");
            riskScore += velocity.getScore();
        }

        RiskRulesProperties.AccountDrain accountDrain =
                properties.getAccountDrain();

        if (amount.doubleValue() > accountDrain.getAmountThreshold()
                && riskScore > accountDrain.getMinimumRiskScore()) {

            reasons.add(new ReasonDetail(
                    accountDrain.getCode(),
                    "Large outbound share of available funds to external account",
                    "CRITICAL"
            ));
            reasonCodes.add("ACCOUNT_DRAIN");
            riskScore += accountDrain.getScore();
        }

        RiskRulesProperties.Rule weakAuthentication =
                properties.getWeakAuthentication();

        if (context != null
                && "NONE".equalsIgnoreCase(
                String.valueOf(context.get("authentication_method")))) {

            reasons.add(new ReasonDetail(
                    weakAuthentication.getCode(),
                    "Weak or missing authentication for high value transaction",
                    "MEDIUM"
            ));
            reasonCodes.add("LOCATION_SHIFT");
            riskScore += weakAuthentication.getScore();
        }

        return new RuleEvaluation(
                Math.min(100, riskScore),
                reasons,
                reasonCodes
        );
    }

    public Decision evaluateDecision(int riskScore) {
        RiskRulesProperties.DecisionPolicy policy =
                properties.getDecisionPolicy();

        if (riskScore >= policy.getBlockThreshold()) {
            return new Decision("BLOCK", "REJECT_TRANSACTION");
        }

        if (riskScore >= policy.getHoldThreshold()) {
            return new Decision("HOLD", "STEP_UP_AUTHENTICATION");
        }

        if (riskScore >= policy.getChallengeThreshold()) {
            return new Decision("CHALLENGE", "STEP_UP_AUTHENTICATION");
        }

        return new Decision("ALLOW", "NONE");
    }

    public String getRulesetVersion() {
        return properties.getRulesetVersion();
    }

    public record RuleEvaluation(
            int riskScore,
            List<ReasonDetail> reasons,
            List<String> reasonCodes
    ) {
    }

    public record Decision(
            String decision,
            String recommendedAction
    ) {
    }
}