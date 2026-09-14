package com.tamvagbackend.controller;

import com.tamvagbackend.dto.RiskDtos;
import com.tamvagbackend.service.RiskEngineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/risk")
public class RiskController {

    private final RiskEngineService riskEngineService;

    public RiskController(RiskEngineService riskEngineService) {
        this.riskEngineService = riskEngineService;
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAuthority('SCOPE_risk:evaluate')")
    public ResponseEntity<RiskDtos.RiskEvaluationResponse> evaluateTransaction(
            @Valid @RequestBody RiskDtos.RiskEvaluationRequest request) {

        RiskDtos.RiskEvaluationResponse response =
                riskEngineService.evaluateTransaction(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{riskEventId}")
    @PreAuthorize("hasAuthority('SCOPE_risk:evaluate')")
    public ResponseEntity<RiskDtos.RiskEvaluationResponse> getRiskEvent(
            @PathVariable UUID riskEventId) {

        RiskDtos.RiskEvaluationResponse response =
                riskEngineService.getRiskEvent(riskEventId);

        return ResponseEntity.ok(response);
    }
}

