package com.tamvagbackend.service;

import com.tamvagbackend.config.RiskRulesProperties;
import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.repository.BeneficiaryRepository;
import com.tamvagbackend.domain.repository.DeviceRepository;
import com.tamvagbackend.domain.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurableRiskRulesEngineTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private RiskRulesProperties properties;

    private ConfigurableRiskRulesEngine rulesEngine;

    @BeforeEach
    void setUp() {
        properties = new RiskRulesProperties();

        rulesEngine = new ConfigurableRiskRulesEngine(
                deviceRepository,
                beneficiaryRepository,
                transactionRepository,
                properties
        );
    }

    @Test
    void evaluate_NewDeviceAndNewBeneficiary() {
        UUID customerId = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        String deviceId = "device-123";
        String beneficiaryId = "beneficiary-123";

        when(deviceRepository.findByCustomerAndFingerprintToken(
                customer,
                deviceId
        )).thenReturn(Optional.empty());

        when(beneficiaryRepository.findByCustomerAndToken(
                customer,
                beneficiaryId
        )).thenReturn(Optional.empty());

        when(transactionRepository.findRecentByCustomer(
                any(UUID.class),
                any(Instant.class)
        )).thenReturn(Collections.emptyList());

        ConfigurableRiskRulesEngine.RuleEvaluation result =
                rulesEngine.evaluate(
                        customer,
                        deviceId,
                        beneficiaryId,
                        BigDecimal.valueOf(500),
                        Collections.emptyMap(),
                        Instant.now()
                );

        assertEquals(55, result.riskScore());
        assertTrue(result.reasonCodes().contains("NEW_DEVICE"));
        assertTrue(result.reasonCodes().contains("NEW_BENEFICIARY"));
    }

    @Test
    void evaluate_NewCustomerLargeTransaction() {
        UUID customerId = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        when(transactionRepository.findRecentByCustomer(
                any(UUID.class),
                any(Instant.class)
        )).thenReturn(Collections.emptyList());

        ConfigurableRiskRulesEngine.RuleEvaluation result =
                rulesEngine.evaluate(
                        customer,
                        null,
                        null,
                        BigDecimal.valueOf(6000),
                        Collections.emptyMap(),
                        Instant.now()
                );

        assertEquals(20, result.riskScore());
        assertTrue(result.reasonCodes().contains("UNUSUAL_AMOUNT"));
    }

    @Test
    void evaluate_WeakAuthentication() {
        UUID customerId = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        when(transactionRepository.findRecentByCustomer(
                any(UUID.class),
                any(Instant.class)
        )).thenReturn(Collections.emptyList());

        ConfigurableRiskRulesEngine.RuleEvaluation result =
                rulesEngine.evaluate(
                        customer,
                        null,
                        null,
                        BigDecimal.valueOf(100),
                        Collections.singletonMap(
                                "authentication_method",
                                "NONE"
                        ),
                        Instant.now()
                );

        assertEquals(15, result.riskScore());
        assertTrue(result.reasonCodes().contains("LOCATION_SHIFT"));
    }

    @Test
    void evaluateDecision_UsesConfiguredThresholds() {
        ConfigurableRiskRulesEngine.Decision allow =
                rulesEngine.evaluateDecision(39);

        ConfigurableRiskRulesEngine.Decision challenge =
                rulesEngine.evaluateDecision(40);

        ConfigurableRiskRulesEngine.Decision hold =
                rulesEngine.evaluateDecision(70);

        ConfigurableRiskRulesEngine.Decision block =
                rulesEngine.evaluateDecision(90);

        assertEquals("ALLOW", allow.decision());
        assertEquals("CHALLENGE", challenge.decision());
        assertEquals("HOLD", hold.decision());
        assertEquals("BLOCK", block.decision());

        assertEquals("NONE", allow.recommendedAction());
        assertEquals("STEP_UP_AUTHENTICATION", challenge.recommendedAction());
        assertEquals("STEP_UP_AUTHENTICATION", hold.recommendedAction());
        assertEquals("REJECT_TRANSACTION", block.recommendedAction());
    }

    @Test
    void evaluateDecision_RespondsToChangedConfiguration() {
        properties.getDecisionPolicy().setChallengeThreshold(20);
        properties.getDecisionPolicy().setHoldThreshold(50);
        properties.getDecisionPolicy().setBlockThreshold(80);

        assertEquals(
                "CHALLENGE",
                rulesEngine.evaluateDecision(20).decision()
        );

        assertEquals(
                "HOLD",
                rulesEngine.evaluateDecision(50).decision()
        );

        assertEquals(
                "BLOCK",
                rulesEngine.evaluateDecision(80).decision()
        );
    }

    @Test
    void rulesetVersionIsConfigurable() {
        assertEquals(
                "rules-2026.09.1",
                rulesEngine.getRulesetVersion()
        );

        properties.setRulesetVersion("rules-test-2.0");

        assertEquals(
                "rules-test-2.0",
                rulesEngine.getRulesetVersion()
        );
    }
}
