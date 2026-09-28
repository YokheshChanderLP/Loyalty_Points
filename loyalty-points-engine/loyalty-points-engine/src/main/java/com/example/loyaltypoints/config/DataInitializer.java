package com.example.loyaltypoints.config;

import com.example.loyaltypoints.entity.Tier;
import com.example.loyaltypoints.repository.TierRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Seeds the default Silver / Gold / Platinum tiers the first time the app starts. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final TierRepository tierRepository;

    public DataInitializer(TierRepository tierRepository) {
        this.tierRepository = tierRepository;
    }

    @Override
    public void run(String... args) {
        if (tierRepository.count() == 0) {
            tierRepository.save(newTier("Silver", 0L, "2.00"));
            tierRepository.save(newTier("Gold", 1000L, "5.00"));
            tierRepository.save(newTier("Platinum", 5000L, "10.00"));
        }
    }

    private Tier newTier(String name, long minPoints, String discount) {
        Tier t = new Tier();
        t.setName(name);
        t.setMinPoints(minPoints);
        t.setDiscountPercent(new BigDecimal(discount));
        return t;
    }
}
