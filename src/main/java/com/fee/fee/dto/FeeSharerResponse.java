package com.fee.fee.dto;

import com.fee.fee.domain.FeeSharer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeSharerResponse {
    private Long id;
    private String sharerType;
    private String sharerId;
    private String sharerName;
    private BigDecimal percentage;
    private Boolean isPrimary;
    private LocalDateTime createdAt;

    public FeeSharerResponse(FeeSharer feeSharer) {
        this.id = feeSharer.getId();
        this.sharerType = feeSharer.getSharerType();
        this.sharerId = feeSharer.getSharerId();
        this.sharerName = feeSharer.getSharerName();
        this.percentage = feeSharer.getPercentage();
        this.isPrimary = feeSharer.getIsPrimary();
        this.createdAt = feeSharer.getCreatedAt();
    }
}