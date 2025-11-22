package com.fee.fee.dto;

import com.fee.fee.enumeration.FeeCalculationType;
import com.fee.fee.enumeration.FeeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateFeeRequest {
    private String tenantId; // Made optional for default tenant

    @NotNull(message = "Fee type is required")
    private FeeType feeType;

    @NotNull(message = "Calculation type is required")
    private FeeCalculationType calculationType;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

//    @NotNull(message = "Transaction type ID is required")
//    private Long transactionTypeId;
//
//    @NotEmpty(message = "At least one channel is required")
//    private List<Long> channelIds;

    @NotBlank(message = "Transaction type name is required")
    private String transactionType; // Changed from transactionTypeId to transactionType name

    @NotEmpty(message = "At least one channel is required")
    private List<String> channels; // Changed from channelIds to channel names

    @DecimalMin(value = "0.0", inclusive = false, message = "Fixed amount must be greater than 0")
    private BigDecimal fixedAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "Percentage rate must be greater than 0")
    private BigDecimal percentageRate;

    @Builder.Default
    private Boolean isShared = false;

    private String productId;

    private String processorId;

    private String clientId;

    @Valid
    private List<FeeRangeRequest> feeRanges;

    @Valid
    private List<FeeSharerRequest> feeSharers;
}