package com.example.loyaltypoints.controller;

import com.example.loyaltypoints.dto.CustomerRequest;
import com.example.loyaltypoints.dto.CustomerResponse;
import com.example.loyaltypoints.dto.LedgerResponse;
import com.example.loyaltypoints.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerResponse> getAll() {
        return customerService.findAll();
    }

    /** Current tier, balance and lifetime points of one customer. */
    @GetMapping("/{id}")
    public CustomerResponse getOne(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Full points history (ledger) of one customer, newest first. */
    @GetMapping("/{id}/points-history")
    public List<LedgerResponse> pointsHistory(@PathVariable Long id) {
        return customerService.getPointsHistory(id);
    }
}
