package com.tamvagbackend.controller;

import com.tamvagbackend.dto.RiskDtos.RiskEvaluationRequest;
import com.tamvagbackend.dto.RiskDtos.RiskEvaluationResponse;
import com.tamvagbackend.service.RiskEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/risk")
@Tag(name = "Risk Engine", description = "Real-time transaction risk evaluation, scoring, and explainability")
public class RiskController {

    private final RiskEngineService riskEngineService;

    public RiskController(RiskEngineService riskEngineService) {
        this.riskEngineService = riskEngineService;
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate transaction risk", description = "Executes deterministic rule catalogue (R001-R008), feature evaluation, scoring (0-100), and returns decision (ALLOW, CHALLENGE, HOLD, BLOCK) with reason codes")
    public ResponseEntity<RiskEvaluationResponse> evaluateTransaction(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RiskEvaluationRequest request
    ) {
        RiskEvaluationResponse response = riskEngineService.evaluateTransaction(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/events/{id}")
    @Operation(summary = "Retrieve risk event", description = "Returns full details of an evaluated risk event including score and audit traceability")
    public ResponseEntity<RiskEvaluationResponse> getRiskEvent(@PathVariable("id") UUID id) {
        RiskEvaluationResponse response = riskEngineService.getRiskEvent(id);
        return ResponseEntity.ok(response);
    }
}
