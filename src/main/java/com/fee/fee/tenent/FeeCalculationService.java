//package com.fee.fee.tenent;
//
//import com.fee.fee.domain.Fee;
//import com.fee.fee.domain.FeeRange;
//import com.fee.fee.domain.FeeSharer;
//import com.fee.fee.domain.Tenant;
//import com.fee.fee.dto.CalculateFeeRequest;
//import com.fee.fee.dto.FeeResponse;
//import com.fee.fee.dto.SharerDistribution;
//import com.fee.fee.repository.FeeRepository;
//import com.fee.fee.repository.FeeSharerRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class FeeCalculationService {
//    private final FeeRepository feeRepo;
//    private final FeeSharerRepository sharerRepo;
//    private final TenantService tenantService;
//
//    public FeeResponse calculateFee(CalculateFeeRequest req) {
//        Tenant tenant = tenantService.resolveTenant(req.getTenant());
//        String txType = req.getTransactionType();
//        String channel = req.getChannel();
//
//        // 1. Resolve Main Fee (CLIENT > PRODUCT > DEFAULT)
//        Fee mainFee = findFee(tenant, txType, channel, req.getClientId(), req.getProductCode(), false)
//                .orElseThrow(() -> new IllegalArgumentException("No main fee found"));
//
//        // 2. Resolve Processor Fee
//        Optional<Fee> processorFee = req.getProcessorName() != null
//                ? findProcessorFee(tenant, txType, channel, req.getClientId(), req.getProductCode(), req.getProcessorName())
//                : null;
//
//        BigDecimal mainAmount = calculateFeeAmount(mainFee, req.getAmount());
//        BigDecimal processorAmount = processorFee != null ? calculateFeeAmount(processorFee, req.getAmount()) : BigDecimal.ZERO;
//        BigDecimal total = mainAmount.add(processorAmount);
//
//        // 3. Sharer Distribution
//        List<FeeSharer> sharers = sharerRepo.findByTenantAndTransactionTypeCodeAndChannelCode(tenant, txType, channel);
//        validateSharersTotal100(sharers);
//
//        BigDecimal shareBase = mainFee.isShareProcessorFee() ? total : mainAmount;
//        List<SharerDistribution> distributions = sharers.stream()
//                .map(s -> {
//                    SharerDistribution dist = new SharerDistribution();
//                    dist.setSharerName(s.getSharerName());
//                    dist.setAmount(shareBase.multiply(s.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
//                    return dist;
//                })
//                .collect(Collectors.toList());
//
//        FeeResponse resp = new FeeResponse();
//        resp.setMainFee(mainAmount);
//        resp.setProcessorFee(processorAmount);
//        resp.setTotalFee(total);
//        resp.setSharerDistributions(distributions);
//        return resp;
//    }
//
//    private Optional<Fee> findFee(Tenant tenant, String txType, String channel, String clientId, String productCode, boolean isProcessor) {
//        // CLIENT
//        if (clientId != null) {
//            List<Fee> fees = feeRepo.findByTenantAndTransactionTypeCodeAndChannelCodeAndFeeLevelAndClientIdAndProductCodeAndIsProcessorFee(
//                    tenant, txType, channel, Fee.FeeLevel.CLIENT, clientId, productCode, isProcessor);
//            if (!fees.isEmpty()) return Optional.of(fees.get(0));
//        }
//
//        // PRODUCT
//        if (productCode != null) {
//            List<Fee> fees = feeRepo.findByTenantAndTransactionTypeCodeAndChannelCodeAndFeeLevelAndProductCodeAndIsProcessorFee(
//                    tenant, txType, channel, Fee.FeeLevel.PRODUCT, productCode, isProcessor);
//            if (!fees.isEmpty()) return Optional.of(fees.get(0));
//        }
//
//        // DEFAULT
//        List<Fee> fees = feeRepo.findByTenantAndTransactionTypeCodeAndChannelCodeAndFeeLevelAndIsProcessorFee(
//                tenant, txType, channel, Fee.FeeLevel.DEFAULT, isProcessor);
//        return fees.isEmpty() ? Optional.empty() : Optional.of(fees.get(0));
//    }
//
//    private Optional<Fee> findProcessorFee(Tenant tenant, String txType, String channel, String clientId, String productCode, String processorName) {
//        return findFee(tenant, txType, channel, clientId, productCode, true)
//                .filter(f -> processorName.equalsIgnoreCase(f.getProcessorName()));
//    }
//
//    private BigDecimal calculateFeeAmount(Fee fee, BigDecimal amount) {
//        return switch (fee.getFeeType()) {
//            case FIXED -> fee.getFixedAmount();
//            case PERCENTAGE -> {
//                BigDecimal calc = amount.multiply(fee.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
//                if (fee.getMinAmount() != null) calc = calc.max(fee.getMinAmount());
//                if (fee.getMaxAmount() != null) calc = calc.min(fee.getMaxAmount());
//                yield calc;
//            }
//            case RANGE -> fee.getRanges().stream()
//                    .filter(r -> (r.getMinTxnAmount() == null || amount.compareTo(r.getMinTxnAmount()) >= 0) &&
//                            (r.getMaxTxnAmount() == null || amount.compareTo(r.getMaxTxnAmount()) <= 0))
//                    .findFirst()
//                    .map(FeeRange::getFeeAmount)
//                    .orElse(BigDecimal.ZERO);
//        };
//    }
//
//    private void validateSharersTotal100(List<FeeSharer> sharers) {
//        if (sharers.isEmpty()) return;
//        BigDecimal total = sharers.stream()
//                .map(FeeSharer::getPercentage)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        if (total.compareTo(BigDecimal.valueOf(100)) != 0) {
//            throw new IllegalStateException("Fee sharers must total 100%, current: " + total);
//        }
//    }
//}