package com.fee.fee.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SharerDistribution {
    private String sharerName;
    private BigDecimal amount;
}
