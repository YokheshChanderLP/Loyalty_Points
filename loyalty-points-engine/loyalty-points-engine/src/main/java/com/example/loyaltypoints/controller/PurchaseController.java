package com.example.loyaltypoints.controller;

import com.example.loyaltypoints.dto.PurchasePreviewResponse;
import com.example.loyaltypoints.dto.PurchaseRequest;
import com.example.loyaltypoints.dto.PurchaseResponse;
import com.example.loyaltypoints.dto.PurchaseUpdateRequest;
import com.example.loyaltypoints.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public List<PurchaseResponse> getAll(@RequestParam(required = false) Long customerId) {
        return purchaseService.findAll(customerId);
    }

    @GetMapping("/{id}")
    public PurchaseResponse getOne(@PathVariable Long id) {
        return purchaseService.findById(id);
    }

    /** Records a purchase: applies the tier discount, redeems points, awards points, upgrades the tier. */
    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.create(request));
    }

    /** Calculates discount / redemption / points without saving anything. */
    @PostMapping("/preview")
    public PurchasePreviewResponse preview(@Valid @RequestBody PurchaseRequest request) {
        return purchaseService.preview(request);
    }

    /** Only the description is editable. */
    @PutMapping("/{id}")
    public PurchaseResponse update(@PathVariable Long id, @Valid @RequestBody PurchaseUpdateRequest request) {
        return purchaseService.updateDescription(id, request);
    }

    /** Cancels the purchase and reverses its points (record is kept with status CANCELLED). */
    @DeleteMapping("/{id}")
    public PurchaseResponse cancel(@PathVariable Long id) {
        return purchaseService.cancel(id);
    }
}
