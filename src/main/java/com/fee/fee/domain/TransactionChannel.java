package com.fee.fee.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class TransactionChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String description;
}