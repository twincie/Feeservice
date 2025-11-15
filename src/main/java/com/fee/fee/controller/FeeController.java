package com.fee.fee.controller;

import com.fee.fee.domain.Fee;
import com.fee.fee.domain.Tenant;
import com.fee.fee.dto.ApiResponse;
import com.fee.fee.repository.FeeRepository;
import com.fee.fee.repository.TransactionChannelRepository;
import com.fee.fee.repository.TransactionTypeRepository;
//import com.fee.fee.tenent.FeeCalculationService;
import com.fee.fee.tenent.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
@Validated
public class FeeController {

//    private final FeeCalculationService feeCalculationService;
    private final FeeRepository feeRepo;
    private final TenantService tenantService;
    private final TransactionTypeRepository typeRepo;
    private final TransactionChannelRepository channelRepo;

    // ==================== CALCULATION ENDPOINTS ====================

//    @PostMapping("/calculate")
//    public ResponseEntity<ApiResponse<FeeResponse>> calculatePost(@Valid @RequestBody CalculateFeeRequest req) {
//        FeeResponse result = feeCalculationService.calculateFee(req);
//        return ResponseEntity.ok(new ApiResponse<>("Fee calculated (POST)", result));
//    }

    // NEW: Additional GET-based calculation (query params)
//    @GetMapping("/calculate")
//    public ResponseEntity<ApiResponse<FeeResponse>> calculateGet(
//            @RequestParam String tenant,
//            @RequestParam String transactionType,
//            @RequestParam String channel,
//            @RequestParam BigDecimal amount,
//            @RequestParam(required = false) String productCode,
//            @RequestParam(required = false) String clientId,
//            @RequestParam(required = false) String processorName) {
//
//        CalculateFeeRequest req = new CalculateFeeRequest();
//        req.setTenant(tenant);
//        req.setTransactionType(transactionType);
//        req.setChannel(channel);
//        req.setAmount(amount);
//        req.setProductCode(productCode);
//        req.setClientId(clientId);
//        req.setProcessorName(processorName);
//
//        FeeResponse result = feeCalculationService.calculateFee(req);
//        return ResponseEntity.ok(new ApiResponse<>("Fee calculated (GET)", result));
//    }

    // ==================== CRUD FOR FEE ====================

    @GetMapping
    public ResponseEntity<ApiResponse<List<Fee>>> list(@RequestParam String tenant) {
        Tenant t = tenantService.resolveTenant(tenant);
        List<Fee> fees = feeRepo.findAll().stream()
                .filter(f -> f.getTenant().getId().equals(t.getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>("success","Fees retrieved", fees));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Fee>> get(@PathVariable Long id, @RequestParam String tenant) {
        Tenant t = tenantService.resolveTenant(tenant);
        Fee fee = feeRepo.findById(id)
                .filter(f -> f.getTenant().getId().equals(t.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Fee not found or access denied"));
        return ResponseEntity.ok(new ApiResponse<>("success","Fee found", fee));
    }

//    @PostMapping
//    public ResponseEntity<ApiResponse<Fee>> create(
//            @Valid @RequestBody FeeCreateRequest request,
//            @RequestParam String tenant) {
//
//        Tenant t = tenantService.resolveTenant(tenant);
//        validateFeeRequest(request, t);
//
//        Fee fee = new Fee();
//        mapFeeRequestToEntity(request, fee, t);
//        Fee saved = feeRepo.save(fee);
//        return ResponseEntity.ok(new ApiResponse<>("Fee created", saved));
//    }

//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<Fee>> update(
//            @PathVariable Long id,
//            @Valid @RequestBody FeeCreateRequest request,
//            @RequestParam String tenant) {
//
//        Tenant t = tenantService.resolveTenant(tenant);
//        Fee existing = feeRepo.findById(id)
//                .filter(f -> f.getTenant().getId().equals(t.getId()))
//                .orElseThrow(() -> new IllegalArgumentException("Fee not found or access denied"));
//
//        validateFeeRequest(request, t);
//        mapFeeRequestToEntity(request, existing, t);
//        Fee updated = feeRepo.save(existing);
//        return ResponseEntity.ok(new ApiResponse<>("Fee updated", updated));
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @RequestParam String tenant) {
//        Tenant t = tenantService.resolveTenant(tenant);
//        Fee fee = feeRepo.findById(id)
//                .filter(f -> f.getTenant().getId().equals(t.getId()))
//                .orElseThrow(() -> new IllegalArgumentException("Fee not found or access denied"));
//        feeRepo.delete(fee);
//        return ResponseEntity.ok(new ApiResponse<>("Fee deleted", null));
//    }
//
//    // ==================== HELPER METHODS ====================
//
//    private void validateFeeRequest(FeeCreateRequest req, Tenant tenant) {
//        typeRepo.findByCode(req.getTransactionTypeCode())
//                .orElseThrow(() -> new IllegalArgumentException("Invalid transaction type: " + req.getTransactionTypeCode()));
//        channelRepo.findByCode(req.getChannelCode())
//                .orElseThrow(() -> new IllegalArgumentException("Invalid channel: " + req.getChannelCode()));
//
//        if (req.getFeeType() == Fee.FeeType.RANGE && (req.getRanges() == null || req.getRanges().isEmpty())) {
//            throw new IllegalArgumentException("RANGE fee must have at least one range");
//        }
//    }
//
//    private void mapFeeRequestToEntity(FeeCreateRequest req, Fee fee, Tenant tenant) {
//        fee.setTenant(tenant);
//        fee.setFeeType(req.getFeeType());
//        fee.setFeeLevel(req.getFeeLevel());
//        fee.setProductCode(req.getProductCode());
//        fee.setClientId(req.getClientId());
//        fee.setTransactionType(typeRepo.findByCode(req.getTransactionTypeCode()).get());
//        fee.setChannel(channelRepo.findByCode(req.getChannelCode()).get());
//        fee.setFixedAmount(req.getFixedAmount());
//        fee.setPercentage(req.getPercentage());
//        fee.setMinAmount(req.getMinAmount());
//        fee.setMaxAmount(req.getMaxAmount());
//        fee.setProcessorFee(req.isProcessorFee());
//        fee.setProcessorName(req.getProcessorName());
//        fee.setShareProcessorFee(req.isShareProcessorFee());
//
//        // Clear and set ranges
//        fee.getRanges().clear();
//        if (req.getRanges() != null) {
//            req.getRanges().forEach(r -> {
//                FeeRange range = new FeeRange();
//                range.setFee(fee);
//                range.setMinTxnAmount(r.getMinTxnAmount());
//                range.setMaxTxnAmount(r.getMaxTxnAmount());
//                range.setFeeAmount(r.getFeeAmount());
//                fee.getRanges().add(range);
//            });
//        }
//    }
}