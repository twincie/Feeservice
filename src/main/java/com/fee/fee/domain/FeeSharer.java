package com.fee.fee.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeSharer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Fee fee;

    @Column(nullable = false)
    private String sharerType; // "PRIMARY", "PARTNER", "AFFILIATE", etc.

    @Column(nullable = false)
    private String sharerId; // Could be user ID, partner ID, etc.

    private String sharerName;

    @Column(nullable = false)
    @DecimalMin(value = "0.0", message = "Percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Percentage cannot exceed 100")
    private BigDecimal percentage;

    @Builder.Default
    private Boolean isPrimary = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}