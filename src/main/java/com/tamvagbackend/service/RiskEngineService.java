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
    private final ConfigurableRiskRulesEngine rulesEngine;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

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
            ConfigurableRiskRulesEngine rulesEngine,
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
        this.rulesEngine = rulesEngine;
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

        ConfigurableRiskRulesEngine.RuleEvaluation ruleEvaluation =
                rulesEngine.evaluate(
                        customer,
                        request.deviceId(),
                        request.destination() != null
                                ? request.destination().identifier()
                                : null,
                        request.amount(),
                        request.context(),
                        now
                );

        int riskScore = ruleEvaluation.riskScore();

        List<ReasonDetail> reasons =
                ruleEvaluation.reasons();

        List<String> reasonCodes =
                ruleEvaluation.reasonCodes();

        ConfigurableRiskRulesEngine.Decision policyDecision =
                rulesEngine.evaluateDecision(riskScore);

        String decision = policyDecision.decision();
        String recommendedAction = policyDecision.recommendedAction();

        String effectiveRulesetVersion = rulesEngine.getRulesetVersion();

        // Persist RiskEvent
        RiskEvent event = new RiskEvent();
        event.setCustomer(customer);
        event.setAccount(account);
        event.setRequestId(UUID.randomUUID().toString());
        event.setRiskScore(riskScore);
        event.setDecision(decision);
        event.setRecommendedAction(recommendedAction);
        event.setReasonCodes(toJson(reasonCodes));
        event.setRulesetVersion(effectiveRulesetVersion);
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
                effectiveRulesetVersion,
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