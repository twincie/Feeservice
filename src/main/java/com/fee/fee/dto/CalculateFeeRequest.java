package com.fee.fee.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CalculateFeeRequest {
    private String tenant;
    private String transactionType;
    private String channel;
    private BigDecimal amount;
    private String productCode;
    private String clientId;
    private String processorName;
}