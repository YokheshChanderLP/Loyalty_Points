package com.example.loyaltypoints.controller;

import com.example.loyaltypoints.dto.TierRequest;
import com.example.loyaltypoints.entity.Tier;
import com.example.loyaltypoints.service.TierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tiers")
public class TierController {

    private final TierService tierService;

    public TierController(TierService tierService) {
        this.tierService = tierService;
    }

    @GetMapping
    public List<Tier> getAll() {
        return tierService.findAll();
    }

    @GetMapping("/{id}")
    public Tier getOne(@PathVariable Long id) {
        return tierService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Tier> create(@Valid @RequestBody TierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tierService.create(request));
    }

    @PutMapping("/{id}")
    public Tier update(@PathVariable Long id, @Valid @RequestBody TierRequest request) {
        return tierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
