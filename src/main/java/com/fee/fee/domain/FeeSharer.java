package com.fee.fee.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
public class FeeSharer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    private String sharerName;
    private BigDecimal percentage;

    @ManyToOne
    private TransactionType transactionType;

    @ManyToOne
    private TransactionChannel channel;
}