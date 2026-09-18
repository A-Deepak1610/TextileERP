package com.textile.erp.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "agent_name", nullable = false)
    private String agentName;

    @Column(length = 50)
    private String mobile;

    @Column(length = 15)
    private String gstin;

    @Column(length = 10)
    private String pan;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "default_commission_type", nullable = false, length = 20)
    private CommissionType defaultCommissionType = CommissionType.PERCENTAGE;

    @Builder.Default
    @Column(name = "default_commission_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultCommissionValue = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
