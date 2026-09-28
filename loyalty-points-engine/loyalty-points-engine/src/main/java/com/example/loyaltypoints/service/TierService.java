package com.example.loyaltypoints.service;

import com.example.loyaltypoints.dto.TierRequest;
import com.example.loyaltypoints.entity.Tier;
import com.example.loyaltypoints.exception.BusinessRuleException;
import com.example.loyaltypoints.exception.DuplicateResourceException;
import com.example.loyaltypoints.exception.ResourceNotFoundException;
import com.example.loyaltypoints.repository.CustomerRepository;
import com.example.loyaltypoints.repository.TierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TierService {

    private final TierRepository tierRepository;
    private final CustomerRepository customerRepository;

    public TierService(TierRepository tierRepository, CustomerRepository customerRepository) {
        this.tierRepository = tierRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<Tier> findAll() {
        return tierRepository.findAllByOrderByMinPointsAsc();
    }

    @Transactional(readOnly = true)
    public Tier findById(Long id) {
        return tierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found with id " + id));
    }

    @Transactional
    public Tier create(TierRequest req) {
        String name = req.name().trim();
        checkUnique(null, name, req.minPoints());
        Tier tier = new Tier();
        apply(tier, name, req);
        return tierRepository.save(tier);
    }

    /**
     * Editing a tier never moves existing customers down: tier changes for customers only
     * happen through upgrades when they earn points.
     */
    @Transactional
    public Tier update(Long id, TierRequest req) {
        Tier tier = findById(id);
        String name = req.name().trim();
        checkUnique(id, name, req.minPoints());
        apply(tier, name, req);
        return tierRepository.save(tier);
    }

    @Transactional
    public void delete(Long id) {
        Tier tier = findById(id);
        long inUse = customerRepository.countByTierId(id);
        if (inUse > 0) {
            throw new BusinessRuleException("Tier '" + tier.getName() + "' is assigned to " + inUse
                    + " customer(s) and cannot be deleted");
        }
        tierRepository.delete(tier);
    }

    private void apply(Tier tier, String name, TierRequest req) {
        tier.setName(name);
        tier.setMinPoints(req.minPoints());
        tier.setDiscountPercent(req.discountPercent());
    }

    private void checkUnique(Long selfId, String name, Long minPoints) {
        tierRepository.findByNameIgnoreCase(name).ifPresent(t -> {
            if (!t.getId().equals(selfId)) {
                throw new DuplicateResourceException("A tier named '" + name + "' already exists");
            }
        });
        tierRepository.findByMinPoints(minPoints).ifPresent(t -> {
            if (!t.getId().equals(selfId)) {
                throw new DuplicateResourceException("Tier '" + t.getName()
                        + "' already uses the threshold of " + minPoints + " points");
            }
        });
    }
}
