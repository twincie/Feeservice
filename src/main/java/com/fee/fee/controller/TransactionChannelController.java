package com.fee.fee.controller;

import com.fee.fee.domain.TransactionChannel;
import com.fee.fee.dto.ApiResponse;
import com.fee.fee.repository.TransactionChannelRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transaction-channels")
@RequiredArgsConstructor
@Validated
public class TransactionChannelController {

    private final TransactionChannelRepository repo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionChannel>>> list() {
        return ResponseEntity.ok(new ApiResponse<>("success", "Channels retrieved", repo.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionChannel>> get(@PathVariable Long id) {
        TransactionChannel channel = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + id));
        return ResponseEntity.ok(new ApiResponse<>("success","Channel found", channel));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionChannel>> create(
            @Valid @RequestBody TransactionChannel channel) {
        if (repo.existsByCode(channel.getCode())) {
            throw new IllegalArgumentException("Channel code already exists: " + channel.getCode());
        }
        TransactionChannel saved = repo.save(channel);
        return ResponseEntity.ok(new ApiResponse<>("fail","Channel created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionChannel>> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionChannel channel) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Channel not found: " + id);
        }
        if (repo.existsByCode(channel.getCode()) && !repo.findByCode(channel.getCode()).get().getId().equals(id)) {
            throw new IllegalArgumentException("Channel code already exists: " + channel.getCode());
        }
        channel.setId(id);
        TransactionChannel updated = repo.save(channel);
        return ResponseEntity.ok(new ApiResponse<>("success","Channel updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Channel not found: " + id);
        }
        repo.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>("success", "Channel deleted", null));
    }
}
