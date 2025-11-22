package com.fee.fee.dto;

import com.fee.fee.domain.Fee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeCalculationResponse {
    private Boolean success;
    private String message;
    private BigDecimal originalAmount;
    private BigDecimal feeAmount;
    private BigDecimal totalAmount;
    private Fee appliedFee;
    private List<FeeSharerCalculation> sharerCalculations;
    private Map<String, Object> calculationDetails;

    @Builder.Default
    private String timestamp = LocalDateTime.now().toString();
}