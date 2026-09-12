package com.tamvagbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.RiskEvent;
import com.tamvagbackend.domain.repository.*;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskEngineServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private RiskEventRepository riskEventRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private CaseRecordRepository caseRecordRepository;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private RiskEngineService riskEngineService;

    private Customer testCustomer;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        testCustomer = new Customer(customerId, "CUS_TEST", "INDIVIDUAL", "ACTIVE");

        ReflectionTestUtils.setField(riskEngineService, "rulesetVersion", "rules-2026.09.1");
        ReflectionTestUtils.setField(riskEngineService, "modelVersion", "fraud-v3.2");
    }

    @Test
    void testEvaluateTransaction_NewDeviceAndNewBeneficiary() throws Exception {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(deviceRepository.findByCustomerAndFingerprintToken(eq(testCustomer), anyString())).thenReturn(Optional.empty());
        when(beneficiaryRepository.findByCustomerAndToken(eq(testCustomer), anyString())).thenReturn(Optional.empty());
        when(transactionRepository.findRecentByCustomer(eq(customerId), any(Instant.class))).thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"NEW_DEVICE\",\"NEW_BENEFICIARY\"]");

        when(riskEventRepository.save(any(RiskEvent.class))).thenAnswer(invocation -> {
            RiskEvent e = invocation.getArgument(0);
            e.setRiskEventId(UUID.randomUUID());
            return e;
        });

        RiskEvaluationRequest request = new RiskEvaluationRequest(
                customerId,
                null,
                new BigDecimal("8500.00"),
                "GHS",
                new DestinationInfo("MOBILE_MONEY", "0240001122", "REF123"),
                "dev_fingerprint_999",
                "MOBILE_APP",
                Instant.now(),
                Map.of("authentication_method", "MFA")
        );

        RiskEvaluationResponse response = riskEngineService.evaluateTransaction(request);

        assertNotNull(response);
        assertTrue(response.riskScore() >= 55); // R001(30) + R002(25) + R003(20) = 75
        assertTrue(List.of("CHALLENGE", "HOLD", "BLOCK").contains(response.decision()));
        assertTrue(response.reasonCodes().contains("NEW_DEVICE"));
        assertTrue(response.reasonCodes().contains("NEW_BENEFICIARY"));
        verify(riskEventRepository, times(1)).save(any(RiskEvent.class));
    }
}
