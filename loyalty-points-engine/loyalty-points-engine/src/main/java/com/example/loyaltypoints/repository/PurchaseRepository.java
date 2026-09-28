package com.example.loyaltypoints.repository;

import com.example.loyaltypoints.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Query("select p from Purchase p join fetch p.customer order by p.purchaseDate desc, p.id desc")
    List<Purchase> findAllWithCustomer();

    @Query("select p from Purchase p join fetch p.customer where p.customer.id = :customerId "
            + "order by p.purchaseDate desc, p.id desc")
    List<Purchase> findByCustomerWithCustomer(@Param("customerId") Long customerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Purchase p where p.customer.id = :customerId")
    void removeByCustomerId(@Param("customerId") Long customerId);
}
