package com.fee.fee.domain;

import com.fee.fee.enumeration.FeeCalculationType;
import com.fee.fee.enumeration.FeeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "fees")
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeType feeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeCalculationType calculationType;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private TransactionType transactionType;

    private BigDecimal fixedAmount;

    private BigDecimal percentageRate;

    @Builder.Default
    private Boolean isShared = false;

    private String productId;

    private String processorId;

    private String clientId;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    @OneToMany(mappedBy = "fee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeeRange> feeRanges = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "fee_channels",
            joinColumns = @JoinColumn(name = "fee_id"),
            inverseJoinColumns = @JoinColumn(name = "channel_id")
    )
    private Set<Channel> channels = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "fee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeeSharer> feeSharers = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addFeeRange(FeeRange feeRange) {
        feeRanges.add(feeRange);
        feeRange.setFee(this);
    }

    public void removeFeeRange(FeeRange feeRange) {
        feeRanges.remove(feeRange);
        feeRange.setFee(null);
    }

    public void addChannel(Channel channel) {
        channels.add(channel);
    }

    public void removeChannel(Channel channel) {
        channels.remove(channel);
    }

    public void addFeeSharer(FeeSharer feeSharer) {
        feeSharers.add(feeSharer);
        feeSharer.setFee(this);
    }

    public void removeFeeSharer(FeeSharer feeSharer) {
        feeSharers.remove(feeSharer);
        feeSharer.setFee(null);
    }
}