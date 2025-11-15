package com.fee.fee.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
public class FeeRange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fee_id")
    private Fee fee;

    private BigDecimal minTxnAmount;
    private BigDecimal maxTxnAmount;
    private BigDecimal feeAmount;
}
