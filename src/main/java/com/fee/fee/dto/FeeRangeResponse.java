package com.fee.fee.dto;

import com.fee.fee.domain.FeeRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeRangeResponse {
    private Long id;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal amount;
    private BigDecimal rate;

    public FeeRangeResponse(FeeRange feeRange) {
        this.id = feeRange.getId();
        this.minAmount = feeRange.getMinAmount();
        this.maxAmount = feeRange.getMaxAmount();
        this.amount = feeRange.getAmount();
        this.rate = feeRange.getRate();
    }
}