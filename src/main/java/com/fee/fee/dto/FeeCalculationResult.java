package com.fee.fee.dto;

import com.fee.fee.domain.Fee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeCalculationResult {
    private Fee fee;
    private BigDecimal calculatedAmount;
    private List<FeeSharerCalculation> sharerCalculations;
}