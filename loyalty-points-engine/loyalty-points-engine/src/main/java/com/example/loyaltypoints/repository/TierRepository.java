package com.example.loyaltypoints.repository;

import com.example.loyaltypoints.entity.Tier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierRepository extends JpaRepository<Tier, Long> {

    Optional<Tier> findByNameIgnoreCase(String name);

    Optional<Tier> findByMinPoints(Long minPoints);

    List<Tier> findAllByOrderByMinPointsAsc();

    /** Lowest tier = the one new customers start in. */
    Optional<Tier> findFirstByOrderByMinPointsAsc();

    /** Highest tier a customer with the given lifetime points qualifies for. */
    Optional<Tier> findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(Long points);

    /** Next tier above the given points. */
    Optional<Tier> findFirstByMinPointsGreaterThanOrderByMinPointsAsc(Long points);
}
