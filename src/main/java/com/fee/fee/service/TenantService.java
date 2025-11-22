package com.fee.fee.service;

import com.fee.fee.domain.Tenant;
import com.fee.fee.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantService {
    private final TenantRepository tenantRepo;
//    @Value("${app.default-tenant}")
    private String defaultTenant;

    public Tenant resolveTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = defaultTenant;
        }
        String finalTenantId = tenantId;
        return tenantRepo.findByTenantId(tenantId)
                .orElseGet(() -> {
                    Tenant newTenant = new Tenant();
                    newTenant.setTenantId(finalTenantId);
                    return tenantRepo.save(newTenant);
                });
    }
}
