package com.fee.fee.controller;

import com.fee.fee.domain.TransactionType;
import com.fee.fee.repository.TransactionTypeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transaction-types")
@RequiredArgsConstructor
public class TransactionTypeController {
    private final TransactionTypeRepository repo;

    @GetMapping
    public List<TransactionType> list() { return repo.findAll(); }

    @PostMapping
    public TransactionType create(@Valid @RequestBody TransactionType type) { return repo.save(type); }

    @PutMapping("/{id}")
    public TransactionType update(@PathVariable Long id, @Valid @RequestBody TransactionType type) {
        type.setId(id);
        return repo.save(type);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { repo.deleteById(id); }
}