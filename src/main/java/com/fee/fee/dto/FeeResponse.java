package com.fee.fee.dto;

import com.fee.fee.domain.Channel;
import com.fee.fee.domain.Fee;
import com.fee.fee.domain.TransactionType;
import com.fee.fee.enumeration.FeeCalculationType;
import com.fee.fee.enumeration.FeeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeResponse {
    private Long id;
    private FeeType feeType;
    private FeeCalculationType calculationType;
    private String name;
    private String description;
    private TransactionType transactionType;
    private Set<Channel> channels;
    private BigDecimal fixedAmount;
    private BigDecimal percentageRate;
    private Boolean isShared;
    private String productId;
    private String processorId;
    private String clientId;
    private Boolean isActive;
    private List<FeeRangeResponse> feeRanges;
    private List<FeeSharerResponse> feeSharers;
    private LocalDateTime createdAt;

    public FeeResponse(Fee fee) {
        this.id = fee.getId();
        this.feeType = fee.getFeeType();
        this.calculationType = fee.getCalculationType();
        this.name = fee.getName();
        this.description = fee.getDescription();
        this.transactionType = fee.getTransactionType();
        this.channels = fee.getChannels();
        this.fixedAmount = fee.getFixedAmount();
        this.percentageRate = fee.getPercentageRate();
        this.isShared = fee.getIsShared();
        this.productId = fee.getProductId();
        this.processorId = fee.getProcessorId();
        this.clientId = fee.getClientId();
        this.isActive = fee.getIsActive();
        this.createdAt = fee.getCreatedAt();

        this.feeRanges = fee.getFeeRanges().stream()
                .map(FeeRangeResponse::new)
                .collect(Collectors.toList());

        this.feeSharers = fee.getFeeSharers().stream()
                .map(FeeSharerResponse::new)
                .collect(Collectors.toList());
    }
}