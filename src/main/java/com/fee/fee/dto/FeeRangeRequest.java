package com.fee.fee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeRangeRequest {
    @NotNull(message = "Min amount is required")
    @DecimalMin(value = "0.0", message = "Min amount must be non-negative")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.0", message = "Max amount must be non-negative")
    private BigDecimal maxAmount;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", message = "Amount must be non-negative")
    private BigDecimal amount;

    @DecimalMin(value = "0.0", message = "Rate must be non-negative")
    private BigDecimal rate;
}