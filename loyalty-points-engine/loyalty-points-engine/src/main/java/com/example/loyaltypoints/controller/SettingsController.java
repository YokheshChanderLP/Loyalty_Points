package com.example.loyaltypoints.controller;

import com.example.loyaltypoints.config.LoyaltyProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final LoyaltyProperties props;

    public SettingsController(LoyaltyProperties props) {
        this.props = props;
    }

    @GetMapping
    public Map<String, BigDecimal> getSettings() {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("pointsPerRupee", props.getPointsPerRupee());
        m.put("redemptionValuePerPoint", props.getRedemptionValuePerPoint());
        return m;
    }
}
