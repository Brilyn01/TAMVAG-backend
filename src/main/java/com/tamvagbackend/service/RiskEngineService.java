package com.tamvagbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.*;
import com.tamvagbackend.domain.repository.*;
import com.tamvagbackend.dto.RiskDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class RiskEngineService {

    private static final Logger log = LoggerFactory.getLogger(RiskEngineService.class);

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RiskEventRepository riskEventRepository;
    private final DeviceRepository deviceRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final CaseRecordRepository caseRecordRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${tamva.ruleset-version:rules-2026.09.1}")
    private String rulesetVersion;

    @Value("${tamva.model-version:fraud-v3.2}")
    private String modelVersion;

    public RiskEngineService(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            RiskEventRepository riskEventRepository,
            DeviceRepository deviceRepository,
            BeneficiaryRepository beneficiaryRepository,
            CaseRecordRepository caseRecordRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.riskEventRepository = riskEventRepository;
        this.deviceRepository = deviceRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.caseRecordRepository = caseRecordRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RiskEvaluationResponse evaluateTransaction(RiskEvaluationRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        Account account = null;
        if (request.accountId() != null) {
            account = accountRepository.findById(request.accountId()).orElse(null);
        }

        Instant now = Instant.now();
        List<ReasonDetail> reasons = new ArrayList<>();
        List<String> reasonCodes = new ArrayList<>();
        int riskScore = 0;

        // Rule R001: NEW_DEVICE
        if (request.deviceId() != null && !request.deviceId().isBlank()) {
            Optional<Device> devOpt = deviceRepository.findByCustomerAndFingerprintToken(customer, request.deviceId());
            if (devOpt.isEmpty() || devOpt.get().getFirstSeenAt().isAfter(now.minus(Duration.ofHours(24)))) {
                reasons.add(new ReasonDetail("R001", "New or unrecognised device used within 24h window", "MEDIUM"));
                reasonCodes.add("NEW_DEVICE");
                riskScore += 30;

                if (devOpt.isEmpty()) {
                    deviceRepository.save(new Device(customer, request.deviceId()));
                }
            }
        }

        // Rule R002: NEW_BENEFICIARY
        if (request.destination() != null && request.destination().identifier() != null) {
            String benToken = request.destination().identifier();
            Optional<Beneficiary> benOpt = beneficiaryRepository.findByCustomerAndToken(customer, benToken);
            if (benOpt.isEmpty() || benOpt.get().getFirstSeenAt().isAfter(now.minus(Duration.ofHours(24)))) {
                reasons.add(new ReasonDetail("R002", "First-time transfer destination within short window", "MEDIUM"));
                reasonCodes.add("NEW_BENEFICIARY");
                riskScore += 25;

                if (benOpt.isEmpty()) {
                    beneficiaryRepository.save(new Beneficiary(customer, benToken));
                }
            }
        }

        // Rule R003: UNUSUAL_AMOUNT
        List<Transaction> recentTxs = transactionRepository.findRecentByCustomer(customer.getCustomerId(), now.minus(Duration.ofDays(90)));
        if (!recentTxs.isEmpty()) {
            double avgAmount = recentTxs.stream().mapToDouble(t -> t.getAmount().doubleValue()).average().orElse(500.0);
            if (request.amount().doubleValue() > (avgAmount * 3.0) && request.amount().doubleValue() > 1000.0) {
                reasons.add(new ReasonDetail("R003", "Transaction amount materially exceeds customer historical baseline", "HIGH"));
                reasonCodes.add("UNUSUAL_AMOUNT");
                riskScore += 25;
            }
        } else if (request.amount().doubleValue() > 5000.0) {
            reasons.add(new ReasonDetail("R003", "Large transaction amount for new customer profile", "HIGH"));
            reasonCodes.add("UNUSUAL_AMOUNT");
            riskScore += 20;
        }

        // Rule R004: VELOCITY_SPIKE
        long count1h = recentTxs.stream()
                .filter(t -> t.getOccurredAt().isAfter(now.minus(Duration.ofHours(1))) && "OUT".equalsIgnoreCase(t.getDirection()))
                .count();
        if (count1h >= 4) {
            reasons.add(new ReasonDetail("R004", "Rapid repeated outbound transactions within rolling 1h window", "HIGH"));
            reasonCodes.add("VELOCITY_SPIKE");
            riskScore += 30;
        }

        // Rule R005: ACCOUNT_DRAIN
        if (request.amount().doubleValue() > 8000.0 && riskScore > 30) {
            reasons.add(new ReasonDetail("R005", "Large outbound share of available funds to external account", "CRITICAL"));
            reasonCodes.add("ACCOUNT_DRAIN");
            riskScore += 20;
        }

        // Rule R006: LOCATION_SHIFT / Auth strength check
        if (request.context() != null && "NONE".equalsIgnoreCase((String) request.context().get("authentication_method"))) {
            reasons.add(new ReasonDetail("R006", "Weak or missing authentication for high value transaction", "MEDIUM"));
            reasonCodes.add("LOCATION_SHIFT");
            riskScore += 15;
        }

        // Cap risk score at 100
        riskScore = Math.min(100, riskScore);

        // Decision Policy
        String decision;
        String recommendedAction;

        if (riskScore >= 90) {
            decision = "BLOCK";
            recommendedAction = "REJECT_TRANSACTION";
        } else if (riskScore >= 70) {
            decision = "HOLD";
            recommendedAction = "STEP_UP_AUTHENTICATION";
        } else if (riskScore >= 40) {
            decision = "CHALLENGE";
            recommendedAction = "STEP_UP_AUTHENTICATION";
        } else {
            decision = "ALLOW";
            recommendedAction = "NONE";
        }

        // Persist RiskEvent
        RiskEvent event = new RiskEvent();
        event.setCustomer(customer);
        event.setAccount(account);
        event.setRequestId(UUID.randomUUID().toString());
        event.setRiskScore(riskScore);
        event.setDecision(decision);
        event.setRecommendedAction(recommendedAction);
        event.setReasonCodes(toJson(reasonCodes));
        event.setRulesetVersion(rulesetVersion);
        event.setModelVersion(modelVersion);
        event.setStatus("EVALUATED");
        event.setCreatedAt(now);
        event.setExpiresAt(now.plus(Duration.ofHours(24)));

        RiskEvent savedEvent = riskEventRepository.save(event);

        // Auto-create operational case if HOLD or BLOCK
        if (riskScore >= 70 || "BLOCK".equals(decision) || "HOLD".equals(decision)) {
            CaseRecord caseRecord = new CaseRecord();
            caseRecord.setRiskEvent(savedEvent);
            caseRecord.setSeverity(riskScore >= 90 ? "CRITICAL" : "HIGH");
            caseRecord.setStatus("OPEN");
            caseRecord.setSource("RULES_ENGINE");
            caseRecord.setNotes("Automated risk alert triggered: " + String.join(", ", reasonCodes));
            caseRecordRepository.save(caseRecord);
        }

        auditService.logEvent(
                "SYSTEM",
                "risk-engine",
                "RISK_EVALUATED",
                "RISK_EVENT",
                savedEvent.getRiskEventId().toString(),
                savedEvent.getRequestId(),
                String.format("Evaluated risk score %d -> decision %s (Reasons: %s)", riskScore, decision, reasonCodes)
        );

        return new RiskEvaluationResponse(
                savedEvent.getRiskEventId(),
                riskScore,
                decision,
                recommendedAction,
                reasons,
                reasonCodes,
                rulesetVersion,
                modelVersion,
                "fs_" + UUID.randomUUID().toString().substring(0, 8),
                savedEvent.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public RiskEvaluationResponse getRiskEvent(UUID riskEventId) {
        RiskEvent event = riskEventRepository.findById(riskEventId)
                .orElseThrow(() -> new IllegalArgumentException("Risk event not found: " + riskEventId));

        List<String> codes = fromJson(event.getReasonCodes());
        List<ReasonDetail> reasons = new ArrayList<>();
        for (String c : codes) {
            reasons.add(new ReasonDetail(c, "Triggered rule: " + c, "HIGH"));
        }

        return new RiskEvaluationResponse(
                event.getRiskEventId(),
                event.getRiskScore(),
                event.getDecision(),
                event.getRecommendedAction(),
                reasons,
                codes,
                event.getRulesetVersion(),
                event.getModelVersion(),
                "fs_snapshot",
                event.getExpiresAt()
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
