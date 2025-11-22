package com.fee.fee.controller;

import com.fee.fee.Util.CorrelationIdContext;
import com.fee.fee.domain.Fee;
import com.fee.fee.domain.FeeSharer;
import com.fee.fee.dto.*;
import com.fee.fee.enumeration.FeeCalculationType;
import com.fee.fee.enumeration.FeeType;
import com.fee.fee.service.FeeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/fees")
@Slf4j
@Validated
public class FeeController {

    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeeResponse>> createFee(
            @RequestParam(required = false) String tenantId, // Optional query parameter
            @Valid @RequestBody CreateFeeRequest request) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            String effectiveTenantId = tenantId != null ? tenantId :
                    (request.getTenantId() != null ? request.getTenantId() : null);

            log.info("Creating fee - Tenant: {}, Transaction Type: {}, Channels: {} - Correlation ID: {}",
                    effectiveTenantId != null ? effectiveTenantId : "default",
                    request.getTransactionType(), request.getChannels(), correlationId);

            Fee fee = feeService.createFee(effectiveTenantId, request);
            FeeResponse response = new FeeResponse(fee);

            ApiResponse<FeeResponse> apiResponse = ApiResponse.success(
                    "Fee created successfully",
                    response
            );

            log.info("Fee creation completed successfully - Correlation ID: {}", correlationId);

            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee creation failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee creation - Correlation ID: {}", correlationId, e);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeeResponse>>> getFees(
            @RequestParam(required = false) String tenantId) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Fetching fees for tenant: {} - Correlation ID: {}",
                    tenantId != null ? tenantId : "default", correlationId);

            List<Fee> fees = feeService.getFeesByTenant(tenantId);
            List<FeeResponse> responses = fees.stream()
                    .map(FeeResponse::new)
                    .collect(Collectors.toList());

            ApiResponse<List<FeeResponse>> apiResponse = ApiResponse.success(
                    "Fees retrieved successfully",
                    responses
            );

            log.info("Retrieved {} fees - Correlation ID: {}", responses.size(), correlationId);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee retrieval failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<List<FeeResponse>> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee retrieval - Correlation ID: {}", correlationId, e);
            ApiResponse<List<FeeResponse>> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeResponse>> getFee(
            @PathVariable Long id,
            @RequestParam(required = false) String tenantId) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Fetching fee ID: {} for tenant: {} - Correlation ID: {}",
                    id, tenantId != null ? tenantId : "default", correlationId);

            Optional<Fee> fee = feeService.getFeeByIdAndTenant(id, tenantId);

            if (fee.isPresent()) {
                FeeResponse response = new FeeResponse(fee.get());
                ApiResponse<FeeResponse> apiResponse = ApiResponse.success(
                        "Fee retrieved successfully",
                        response
                );
                log.info("Fee retrieved successfully - ID: {} - Correlation ID: {}", id, correlationId);
                return ResponseEntity.ok(apiResponse);
            } else {
                log.warn("Fee not found - ID: {} - Correlation ID: {}", id, correlationId);
                ApiResponse<FeeResponse> apiResponse = ApiResponse.error("Fee not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Fee retrieval failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee retrieval - Correlation ID: {}", correlationId, e);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<FeeResponse>> activateFee(
            @PathVariable Long id,
            @RequestParam(required = false) String tenantId) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Activating fee ID: {} for tenant: {} - Correlation ID: {}",
                    id, tenantId != null ? tenantId : "default", correlationId);

            Fee fee = feeService.activateFee(id, tenantId);
            FeeResponse response = new FeeResponse(fee);

            ApiResponse<FeeResponse> apiResponse = ApiResponse.success(
                    "Fee activated successfully",
                    response
            );

            log.info("Fee activated successfully - ID: {} - Correlation ID: {}", id, correlationId);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee activation failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee activation - Correlation ID: {}", correlationId, e);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<FeeResponse>> deactivateFee(
            @PathVariable Long id,
            @RequestParam(required = false) String tenantId) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Deactivating fee ID: {} for tenant: {} - Correlation ID: {}",
                    id, tenantId != null ? tenantId : "default", correlationId);

            Fee fee = feeService.deactivateFee(id, tenantId);
            FeeResponse response = new FeeResponse(fee);

            ApiResponse<FeeResponse> apiResponse = ApiResponse.success(
                    "Fee deactivated successfully",
                    response
            );

            log.info("Fee deactivated successfully - ID: {} - Correlation ID: {}", id, correlationId);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee deactivation failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee deactivation - Correlation ID: {}", correlationId, e);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeResponse>> updateFee(
            @PathVariable Long id,
            @RequestParam(required = false) String tenantId,
            @Valid @RequestBody UpdateFeeRequest request) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Updating fee ID: {} for tenant: {} - Correlation ID: {}",
                    id, tenantId != null ? tenantId : "default", correlationId);

            Fee fee = feeService.updateFee(id, tenantId, request);
            FeeResponse response = new FeeResponse(fee);

            ApiResponse<FeeResponse> apiResponse = ApiResponse.success(
                    "Fee updated successfully",
                    response
            );

            log.info("Fee updated successfully - ID: {} - Correlation ID: {}", id, correlationId);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee update failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee update - Correlation ID: {}", correlationId, e);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeResponse>> partialUpdateFee(
            @PathVariable Long id,
            @RequestParam(required = false) String tenantId,
            @RequestBody Map<String, Object> updates) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Partial updating fee ID: {} for tenant: {} - Correlation ID: {}",
                    id, tenantId != null ? tenantId : "default", correlationId);

            Fee fee = feeService.partialUpdateFee(id, tenantId, updates);
            FeeResponse response = new FeeResponse(fee);

            ApiResponse<FeeResponse> apiResponse = ApiResponse.success(
                    "Fee partially updated successfully",
                    response
            );

            log.info("Fee partially updated successfully - ID: {} - Correlation ID: {}", id, correlationId);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee partial update failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee partial update - Correlation ID: {}", correlationId, e);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @PutMapping("/{id}/sharers")
    public ResponseEntity<ApiResponse<FeeResponse>> updateFeeSharers(
            @PathVariable Long id,
            @RequestParam(required = false) String tenantId,
            @Valid @RequestBody List<FeeSharerRequest> sharerRequests) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Updating fee sharers for fee ID: {} for tenant: {} - Correlation ID: {}",
                    id, tenantId != null ? tenantId : "default", correlationId);

            Fee fee = feeService.updateFeeSharers(id, tenantId, sharerRequests);
            FeeResponse response = new FeeResponse(fee);

            ApiResponse<FeeResponse> apiResponse = ApiResponse.success(
                    "Fee sharers updated successfully",
                    response
            );

            log.info("Fee sharers updated successfully - ID: {} - Correlation ID: {}", id, correlationId);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee sharers update failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee sharers update - Correlation ID: {}", correlationId, e);
            ApiResponse<FeeResponse> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping("/{id}/sharers")
    public ResponseEntity<ApiResponse<List<FeeSharerResponse>>> getFeeSharers(
            @PathVariable Long id,
            @RequestParam(required = false) String tenantId) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Fetching fee sharers for fee ID: {} for tenant: {} - Correlation ID: {}",
                    id, tenantId != null ? tenantId : "default", correlationId);

            List<FeeSharer> feeSharers = feeService.getFeeSharers(id, tenantId);
            List<FeeSharerResponse> responses = feeSharers.stream()
                    .map(FeeSharerResponse::new)
                    .collect(Collectors.toList());

            ApiResponse<List<FeeSharerResponse>> apiResponse = ApiResponse.success(
                    "Fee sharers retrieved successfully",
                    responses
            );

            log.info("Retrieved {} fee sharers for fee ID: {} - Correlation ID: {}",
                    responses.size(), id, correlationId);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee sharers retrieval failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<List<FeeSharerResponse>> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee sharers retrieval - Correlation ID: {}", correlationId, e);
            ApiResponse<List<FeeSharerResponse>> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FeeResponse>>> searchFees(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) FeeType feeType,
            @RequestParam(required = false) FeeCalculationType calculationType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isShared) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Searching fees - Tenant: {}, Type: {}, Calculation: {}, Active: {}, Shared: {} - Correlation ID: {}",
                    tenantId != null ? tenantId : "default", feeType, calculationType, isActive, isShared, correlationId);

            // This would require additional service methods for advanced search
            // For now, we'll get all fees and filter in memory (not recommended for large datasets)
            List<Fee> fees = feeService.getFeesByTenant(tenantId);

            // Apply filters
            List<Fee> filteredFees = fees.stream()
                    .filter(fee -> feeType == null || fee.getFeeType() == feeType)
                    .filter(fee -> calculationType == null || fee.getCalculationType() == calculationType)
                    .filter(fee -> isActive == null || fee.getIsActive().equals(isActive))
                    .filter(fee -> isShared == null || fee.getIsShared().equals(isShared))
                    .collect(Collectors.toList());

            List<FeeResponse> responses = filteredFees.stream()
                    .map(FeeResponse::new)
                    .collect(Collectors.toList());

            ApiResponse<List<FeeResponse>> apiResponse = ApiResponse.success(
                    "Fees search completed successfully",
                    responses
            );

            log.info("Search completed - Found {} fees - Correlation ID: {}", responses.size(), correlationId);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee search failed: {} - Correlation ID: {}", e.getMessage(), correlationId);
            ApiResponse<List<FeeResponse>> apiResponse = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during fee search - Correlation ID: {}", correlationId, e);
            ApiResponse<List<FeeResponse>> apiResponse = ApiResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<FeeCalculationResponse>> calculateFee(
            @Valid @RequestBody FeeCalculationRequest request) {

        String correlationId = CorrelationIdContext.getCorrelationId();

        try {
            log.info("Calculating fee - Tenant: {}, Transaction: {}, Channel: {}, Amount: {} - Correlation ID: {}",
                    request.getTenantId() != null ? request.getTenantId() : "default",
                    request.getTransactionType(), request.getChannel(), request.getAmount(), correlationId);

            FeeCalculationResult result = feeService.calculateFee(request);

            FeeCalculationResponse response = buildCalculationResponse(result, request.getAmount());

            ApiResponse<FeeCalculationResponse> apiResponse = ApiResponse.success(
                    "Fee calculated successfully",
                    response
            );

            log.info("Fee calculation completed successfully - Applied Fee: {} - Correlation ID: {}",
                    result.getFee().getFeeType(), correlationId);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Fee calculation failed: {} - Correlation ID: {}", e.getMessage(), correlationId);

            FeeCalculationResponse errorResponse = FeeCalculationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .originalAmount(request.getAmount())
                    .feeAmount(BigDecimal.ZERO)
                    .totalAmount(request.getAmount())
                    .build();

            ApiResponse<FeeCalculationResponse> apiResponse = ApiResponse.error(e.getMessage(), errorResponse);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);

        } catch (Exception e) {
            log.error("Unexpected error during fee calculation - Correlation ID: {}", correlationId, e);

            FeeCalculationResponse errorResponse = FeeCalculationResponse.builder()
                    .success(false)
                    .message("Internal server error during fee calculation")
                    .originalAmount(request.getAmount())
                    .feeAmount(BigDecimal.ZERO)
                    .totalAmount(request.getAmount())
                    .build();

            ApiResponse<FeeCalculationResponse> apiResponse = ApiResponse.error("Internal server error", errorResponse);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    private FeeCalculationResponse buildCalculationResponse(FeeCalculationResult result, BigDecimal originalAmount) {
        BigDecimal totalAmount = originalAmount.add(result.getCalculatedAmount());

        Map<String, Object> calculationDetails = new HashMap<>();
        calculationDetails.put("calculationType", result.getFee().getCalculationType());
        calculationDetails.put("feeType", result.getFee().getFeeType());
        calculationDetails.put("isShared", result.getFee().getIsShared());
        calculationDetails.put("appliedFeeId", result.getFee().getId());
        calculationDetails.put("appliedFeeName", result.getFee().getName());

        if (result.getFee().getCalculationType() == FeeCalculationType.FIXED) {
            calculationDetails.put("fixedAmount", result.getFee().getFixedAmount());
        } else if (result.getFee().getCalculationType() == FeeCalculationType.PERCENTAGE) {
            calculationDetails.put("percentageRate", result.getFee().getPercentageRate());
        }

        // Add specific reference if applicable
        if (result.getFee().getProductId() != null) {
            calculationDetails.put("productId", result.getFee().getProductId());
        }
        if (result.getFee().getProcessorId() != null) {
            calculationDetails.put("processorId", result.getFee().getProcessorId());
        }
        if (result.getFee().getClientId() != null) {
            calculationDetails.put("clientId", result.getFee().getClientId());
        }

        return FeeCalculationResponse.builder()
                .success(true)
                .message("Fee calculated successfully")
                .originalAmount(originalAmount)
                .feeAmount(result.getCalculatedAmount())
                .totalAmount(totalAmount)
                .appliedFee(result.getFee())
                .sharerCalculations(result.getSharerCalculations())
                .calculationDetails(calculationDetails)
                .build();
    }
}