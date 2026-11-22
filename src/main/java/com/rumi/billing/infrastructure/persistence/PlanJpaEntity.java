package com.rumi.billing.infrastructure.persistence;

import com.rumi.billing.domain.model.BillingCycle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Plans are reference data created by docs/schema.sql; the service only reads them.
 */
@Entity
@Table(name = "plans")
public class PlanJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 10)
    private BillingCycle billingCycle;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "sensor_limit", nullable = false)
    private int sensorLimit;

    protected PlanJpaEntity() {
    }

    public PlanJpaEntity(UUID id, String code, String name, BillingCycle billingCycle, BigDecimal price,
                         String currency, int sensorLimit) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.billingCycle = billingCycle;
        this.price = price;
        this.currency = currency;
        this.sensorLimit = sensorLimit;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public int getSensorLimit() {
        return sensorLimit;
    }
}
