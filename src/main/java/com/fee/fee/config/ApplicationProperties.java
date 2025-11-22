package com.fee.fee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationProperties {
    private TenantConfig tenant = new TenantConfig();

    @Data
    public static class TenantConfig {
        private String defaultTenantId = "default-tenant";
        private String defaultTenantName = "Default Tenant";
    }
}