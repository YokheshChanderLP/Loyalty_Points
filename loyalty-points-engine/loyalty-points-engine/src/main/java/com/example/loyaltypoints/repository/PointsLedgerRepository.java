package com.example.loyaltypoints.repository;

import com.example.loyaltypoints.entity.PointsLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {

    List<PointsLedger> findByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PointsLedger l where l.customer.id = :customerId")
    void removeByCustomerId(@Param("customerId") Long customerId);
}
