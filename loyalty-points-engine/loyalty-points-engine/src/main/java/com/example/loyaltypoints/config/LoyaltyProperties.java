package com.example.loyaltypoints.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Configurable loyalty rules, read from application.properties (prefix app.loyalty). */
@Component
@ConfigurationProperties(prefix = "app.loyalty")
public class LoyaltyProperties {

    private BigDecimal pointsPerRupee = new BigDecimal("0.1");
    private BigDecimal redemptionValuePerPoint = BigDecimal.ONE;

    public BigDecimal getPointsPerRupee() { return pointsPerRupee; }
    public void setPointsPerRupee(BigDecimal pointsPerRupee) { this.pointsPerRupee = pointsPerRupee; }
    public BigDecimal getRedemptionValuePerPoint() { return redemptionValuePerPoint; }
    public void setRedemptionValuePerPoint(BigDecimal v) { this.redemptionValuePerPoint = v; }
}
