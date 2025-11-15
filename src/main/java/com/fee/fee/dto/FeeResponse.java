package com.fee.fee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FeeResponse {
    private BigDecimal mainFee;
    private BigDecimal processorFee;
    private BigDecimal totalFee;
    private List<SharerDistribution> sharerDistributions;
}
