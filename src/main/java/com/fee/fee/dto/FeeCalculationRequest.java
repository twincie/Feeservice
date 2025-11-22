package com.fee.fee.dto;

import com.fee.fee.enumeration.FeeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeCalculationRequest {
    private String tenantId;

    @NotNull(message = "Fee type is required")
    private FeeType feeType;

//    @NotNull(message = "Transaction type ID is required")
//    private Long transactionTypeId;
//
//    @NotNull(message = "Channel ID is required")
//    private Long channelId;

    @NotBlank(message = "Transaction type name is required")
    private String transactionType; // Changed from transactionTypeId to transactionType name

    @NotBlank(message = "Channel name is required")
    private String channel; // Changed from channelId to channel name

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String productId;
    private String processorId;
    private String clientId;

    @Builder.Default
    private Boolean applySharing = true;

    private Map<String, Object> metadata;
}