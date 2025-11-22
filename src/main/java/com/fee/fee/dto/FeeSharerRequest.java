package com.fee.fee.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class FeeSharerRequest {
    @NotBlank(message = "Sharer type is required")
    private String sharerType;

    @NotBlank(message = "Sharer ID is required")
    private String sharerId;

    private String sharerName;

    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Percentage must be greater than 0")
    @DecimalMax(value = "100.0", message = "Percentage cannot exceed 100")
    private BigDecimal percentage;

    @Builder.Default
    private Boolean isPrimary = false;
}