package com.fee.fee.controller;

import com.fee.fee.domain.Channel;
import com.fee.fee.dto.ApiResponse;
import com.fee.fee.repository.ChannelRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transaction-channels")
@RequiredArgsConstructor
@Validated
public class TransactionChannelController {

    private final ChannelRepository repo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Channel>>> list() {
        return ResponseEntity.ok(new ApiResponse<>("success", "Channels retrieved", repo.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Channel>> get(@PathVariable Long id) {
        Channel channel = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + id));
        return ResponseEntity.ok(new ApiResponse<>("success","Channel found", Collections.singletonList(channel)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Channel>> create(
            @Valid @RequestBody Channel channel) {
        if (repo.existsByCode(channel.getCode())) {
            throw new IllegalArgumentException("Channel code already exists: " + channel.getCode());
        }
        Channel saved = repo.save(channel);
        return ResponseEntity.ok(new ApiResponse<>("fail","Channel created", Collections.singletonList(saved)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Channel>> update(
            @PathVariable Long id,
            @Valid @RequestBody Channel channel) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Channel not found: " + id);
        }
        if (repo.existsByCode(channel.getCode()) && !repo.findByCode(channel.getCode()).getFirst().getId().equals(id)) {
            throw new IllegalArgumentException("Channel code already exists: " + channel.getCode());
        }
        channel.setId(id);
        Channel updated = repo.save(channel);
        return ResponseEntity.ok(new ApiResponse<>("success","Channel updated", Collections.singletonList(updated)));
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
