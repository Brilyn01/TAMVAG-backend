package com.tamvagbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Institution;
import com.tamvagbackend.domain.entity.RiskEvent;
import com.tamvagbackend.domain.repository.AccountRepository;
import com.tamvagbackend.domain.repository.BeneficiaryRepository;
import com.tamvagbackend.domain.repository.CaseRecordRepository;
import com.tamvagbackend.domain.repository.CustomerRepository;
import com.tamvagbackend.domain.repository.DeviceRepository;
import com.tamvagbackend.domain.repository.RiskEventRepository;
import com.tamvagbackend.domain.repository.TransactionRepository;
import com.tamvagbackend.dto.RiskDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskEngineServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RiskEventRepository riskEventRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private CaseRecordRepository caseRecordRepository;

    @Mock
    private ConfigurableRiskRulesEngine rulesEngine;

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RiskEngineService riskEngineService;

    private Customer testCustomer;
    private UUID customerId;
    private Account testAccount;
    private UUID accountId;
    private UUID institutionId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        institutionId = UUID.randomUUID();

        testCustomer = new Customer(
                customerId,
                "CUS_TEST",
                "INDIVIDUAL",
                "ACTIVE"
        );

        Institution testInstitution = new Institution(
                institutionId,
                "GCB Bank",
                "BANK",
                "ACTIVE",
                null
        );

        testAccount = new Account();
        testAccount.setAccountId(accountId);
        testAccount.setCustomer(testCustomer);
        testAccount.setInstitution(testInstitution);
        testAccount.setAccountType("MOBILE_MONEY");
        testAccount.setCurrency("GHS");
        testAccount.setStatus("ACTIVE");

        ReflectionTestUtils.setField(
                riskEngineService,
                "modelVersion",
                "fraud-v3.2"
        );
    }

    @Test
    void testEvaluateTransaction_NewDeviceAndNewBeneficiary() throws Exception {
        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(testCustomer));

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(testAccount));

        ConfigurableRiskRulesEngine.RuleEvaluation ruleEvaluation =
                new ConfigurableRiskRulesEngine.RuleEvaluation(
                        75,
                        List.of(
                                new ReasonDetail(
                                        "R001",
                                        "New or unrecognised device used within configured window",
                                        "MEDIUM"
                                ),
                                new ReasonDetail(
                                        "R002",
                                        "First-time transfer destination within configured window",
                                        "MEDIUM"
                                ),
                                new ReasonDetail(
                                        "R003",
                                        "Large transaction amount for new customer profile",
                                        "HIGH"
                                )
                        ),
                        List.of(
                                "NEW_DEVICE",
                                "NEW_BENEFICIARY",
                                "UNUSUAL_AMOUNT"
                        )
                );

        when(rulesEngine.evaluate(
                eq(testCustomer),
                eq("dev_fingerprint_999"),
                eq("0240001122"),
                eq(new BigDecimal("8500.00")),
                any(),
                any(Instant.class)
        )).thenReturn(ruleEvaluation);

        when(rulesEngine.evaluateDecision(75))
                .thenReturn(
                        new ConfigurableRiskRulesEngine.Decision(
                                "HOLD",
                                "STEP_UP_AUTHENTICATION"
                        )
                );

        when(rulesEngine.getRulesetVersion())
                .thenReturn("rules-2026.09.1");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn(
                        "[\"NEW_DEVICE\",\"NEW_BENEFICIARY\",\"UNUSUAL_AMOUNT\"]"
                );

        when(riskEventRepository.save(any(RiskEvent.class)))
                .thenAnswer(invocation -> {
                    RiskEvent event = invocation.getArgument(0);
                    event.setRiskEventId(UUID.randomUUID());
                    return event;
                });

        RiskEvaluationRequest request = new RiskEvaluationRequest(
                customerId,
                accountId,
                new BigDecimal("8500.00"),
                "GHS",
                new DestinationInfo(
                        "MOBILE_MONEY",
                        "0240001122",
                        "REF123"
                ),
                "dev_fingerprint_999",
                "MOBILE_APP",
                Instant.now(),
                Map.of(
                        "authentication_method",
                        "MFA"
                )
        );

        RiskEvaluationResponse response =
                riskEngineService.evaluateTransaction(request);

        assertNotNull(response);

        assertEquals(75, response.riskScore());
        assertEquals("HOLD", response.decision());
        assertEquals(
                "STEP_UP_AUTHENTICATION",
                response.recommendedAction()
        );

        assertTrue(
                response.reasonCodes().contains("NEW_DEVICE")
        );

        assertTrue(
                response.reasonCodes().contains("NEW_BENEFICIARY")
        );

        assertTrue(
                response.reasonCodes().contains("UNUSUAL_AMOUNT")
        );

        assertEquals(
                "rules-2026.09.1",
                response.rulesetVersion()
        );

        assertEquals(
                "fraud-v3.2",
                response.modelVersion()
        );

        assertNotNull(response.riskEventId());
        assertNotNull(response.expiresAt());

        verify(rulesEngine, times(1)).evaluate(
                eq(testCustomer),
                eq("dev_fingerprint_999"),
                eq("0240001122"),
                eq(new BigDecimal("8500.00")),
                any(),
                any(Instant.class)
        );

        verify(rulesEngine, times(1))
                .evaluateDecision(75);

        verify(rulesEngine, times(1))
                .getRulesetVersion();

        verify(riskEventRepository, times(1))
                .save(any(RiskEvent.class));

        verify(caseRecordRepository, times(1))
                .save(any());

        verify(auditService, times(1)).logEvent(
                eq("SYSTEM"),
                eq("risk-engine"),
                eq("RISK_EVALUATED"),
                eq("RISK_EVENT"),
                anyString(),
                anyString(),
                anyString()
        );
    }
}