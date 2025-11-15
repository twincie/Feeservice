package com.fee.fee.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Fee {
    public enum FeeType { FIXED, PERCENTAGE, RANGE }
    public enum FeeLevel { DEFAULT, PRODUCT, CLIENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    private FeeType feeType;

    @Enumerated(EnumType.STRING)
    private FeeLevel feeLevel;

    private String productCode;
    private String clientId;

    @ManyToOne
    private TransactionType transactionType;

    @ManyToOne
    private TransactionChannel channel;

    // Common fields
    private BigDecimal fixedAmount;
    private BigDecimal percentage;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    // For RANGE
    @OneToMany(mappedBy = "fee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeeRange> ranges = new ArrayList<>();

    // Processor
    private boolean isProcessorFee = false;
    private String processorName;

    // Sharing
    private boolean shareProcessorFee = false;
}
