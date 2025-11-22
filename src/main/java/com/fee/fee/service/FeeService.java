package com.fee.fee.service;

import com.fee.fee.Util.CorrelationIdContext;
import com.fee.fee.config.ApplicationProperties;
import com.fee.fee.domain.*;
import com.fee.fee.dto.*;
import com.fee.fee.enumeration.FeeCalculationType;
import com.fee.fee.enumeration.FeeType;
import com.fee.fee.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class FeeService {

    private final FeeRepository feeRepository;
    private final TenantRepository tenantRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final ChannelRepository channelRepository;
    private final FeeRangeRepository feeRangeRepository;
    private final FeeSharerRepository feeSharerRepository;
    private final ApplicationProperties applicationProperties;

    public FeeService(FeeRepository feeRepository,
                      TenantRepository tenantRepository,
                      TransactionTypeRepository transactionTypeRepository,
                      ChannelRepository channelRepository,
                      FeeRangeRepository feeRangeRepository,
                      FeeSharerRepository feeSharerRepository,
                      ApplicationProperties applicationProperties) {
        this.feeRepository = feeRepository;
        this.tenantRepository = tenantRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.channelRepository = channelRepository;
        this.feeRangeRepository = feeRangeRepository;
        this.feeSharerRepository = feeSharerRepository;
        this.applicationProperties = applicationProperties;
    }

    public Fee createFee(String tenantId, CreateFeeRequest request) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(tenantId, request.getTenantId());
        log.info("Creating fee for tenant: {} - Correlation ID: {}", effectiveTenantId, correlationId);

        Tenant tenant = getOrCreateTenant(effectiveTenantId, correlationId);

        // Validate fee name uniqueness per tenant
        if (feeRepository.existsByTenantAndName(tenant, request.getName())) {
            log.warn("Fee name already exists: {} for tenant: {} - Correlation ID: {}",
                    request.getName(), effectiveTenantId, correlationId);
            throw new IllegalArgumentException("Fee with name '" + request.getName() + "' already exists for this tenant");
        }

        // Validate and get transaction type by name
        TransactionType transactionType = transactionTypeRepository
                .findByNameAndIsActive(request.getTransactionType(), true)
                .orElseThrow(() -> {
                    log.warn("Transaction type not found: {} for tenant: {} - Correlation ID: {}",
                            request.getTransactionType(), effectiveTenantId, correlationId);
                    return new IllegalArgumentException("Transaction type '" + request.getTransactionType() + "' not found or inactive");
                });

        // Validate and get channels by names
        Set<Channel> channels = validateAndGetChannelsByName(tenant, request.getChannels(), correlationId);

        // Validate fee type specific requirements
        validateFeeRequest(request, correlationId);

        // Validate fee sharers if fee is shared
        if (request.getIsShared() && request.getFeeSharers() != null) {
            validateFeeSharers(request.getFeeSharers(), correlationId);
        }

        Fee fee = Fee.builder()
                .tenant(tenant)
                .feeType(request.getFeeType())
                .calculationType(request.getCalculationType())
                .name(request.getName())
                .description(request.getDescription())
                .transactionType(transactionType)
                .isShared(request.getIsShared())
                .build();

        // Set channels
        channels.forEach(fee::addChannel);

        // Set calculation type specific fields
        setCalculationSpecificFields(fee, request);

        // Set fee type specific references
        setFeeTypeSpecificReferences(fee, request);

        Fee savedFee = feeRepository.save(fee);

        // Create fee ranges for RANGED calculation type
        if (request.getCalculationType() == FeeCalculationType.RANGED &&
                request.getFeeRanges() != null) {
            createFeeRanges(savedFee, request.getFeeRanges());
        }

        // Create fee sharers if fee is shared
        if (request.getIsShared() && request.getFeeSharers() != null) {
            createFeeSharers(savedFee, request.getFeeSharers(), correlationId);
        }

        log.info("Fee created successfully with ID: {} - Correlation ID: {}",
                savedFee.getId(), correlationId);

        return savedFee;
    }

    private String getEffectiveTenantId(String methodTenantId, String requestTenantId) {
        // Priority: Method parameter > Request body > Default from properties
        if (methodTenantId != null && !methodTenantId.trim().isEmpty()) {
            return methodTenantId;
        }
        if (requestTenantId != null && !requestTenantId.trim().isEmpty()) {
            return requestTenantId;
        }
        return applicationProperties.getTenant().getDefaultTenantId();
    }

    private Tenant getOrCreateTenant(String tenantId, String correlationId) {
        return tenantRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    log.info("Creating default tenant: {} - Correlation ID: {}", tenantId, correlationId);
                    Tenant newTenant = Tenant.builder()
                            .tenantId(tenantId)
                            .name(applicationProperties.getTenant().getDefaultTenantName())
                            .build();
                    return tenantRepository.save(newTenant);
                });
    }

    private Set<Channel> validateAndGetChannelsByName(Tenant tenant, List<String> channelNames, String correlationId) {
        if (channelNames == null || channelNames.isEmpty()) {
            log.warn("No channels provided - Correlation ID: {}", correlationId);
            throw new IllegalArgumentException("At least one channel must be provided");
        }

        Set<Channel> channels = channelRepository
                .findByNameInAndIsActive(channelNames, true);

        if (channels.size() != channelNames.size()) {
            // Find which channels are missing
            Set<String> foundChannelNames = channels.stream()
                    .map(Channel::getName)
                    .collect(Collectors.toSet());

            List<String> missingChannels = channelNames.stream()
                    .filter(name -> !foundChannelNames.contains(name))
                    .collect(Collectors.toList());

            log.warn("One or more channels not found. Missing: {} - Correlation ID: {}", missingChannels, correlationId);
            throw new IllegalArgumentException("One or more channels not found or inactive: " + missingChannels);
        }

        log.debug("Found {} channels for tenant: {} - Correlation ID: {}",
                channels.size(), tenant.getTenantId(), correlationId);

        return channels;
    }

    private void validateFeeRequest(CreateFeeRequest request, String correlationId) {
        log.debug("Validating fee request - Type: {}, Calculation: {} - Correlation ID: {}",
                request.getFeeType(), request.getCalculationType(), correlationId);

        // If fee is shared, validate sharers
        if (request.getIsShared()) {
            if (request.getFeeSharers() == null || request.getFeeSharers().isEmpty()) {
                throw new IllegalArgumentException("Fee sharers must be provided when fee is shared");
            }
        }

        // If fee is not shared, but sharers are provided, throw error
        if (!request.getIsShared() && request.getFeeSharers() != null && !request.getFeeSharers().isEmpty()) {
            throw new IllegalArgumentException("Fee sharers should not be provided when fee is not shared");
        }

        // Validate calculation type specific fields
        switch (request.getCalculationType()) {
            case FIXED:
                if (request.getFixedAmount() == null || request.getFixedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Fixed amount must be provided and greater than 0 for FIXED fees");
                }
                break;
            case PERCENTAGE:
                if (request.getPercentageRate() == null || request.getPercentageRate().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Percentage rate must be provided and greater than 0 for PERCENTAGE fees");
                }
                break;
            case RANGED:
                if (request.getFeeRanges() == null || request.getFeeRanges().isEmpty()) {
                    throw new IllegalArgumentException("Fee ranges must be provided for RANGED fees");
                }
                validateFeeRanges(request.getFeeRanges(), correlationId);
                break;
        }

        // Validate fee type specific references
        switch (request.getFeeType()) {
            case PRODUCT:
                if (request.getProductId() == null) {
                    throw new IllegalArgumentException("productId must be provided for PRODUCT fees");
                }
                break;
            case PROCESSOR:
                if (request.getProcessorId() == null) {
                    throw new IllegalArgumentException("processorId must be provided for PROCESSOR fees");
                }
                break;
            case CLIENT:
                if (request.getClientId() == null) {
                    throw new IllegalArgumentException("clientId must be provided for CLIENT fees");
                }
                break;
            case DEFAULT:
                // No specific references needed for default fees
                break;
        }
    }

    private void validateFeeRanges(List<FeeRangeRequest> feeRanges, String correlationId) {
        List<FeeRangeRequest> sortedRanges = feeRanges.stream()
                .sorted(Comparator.comparing(FeeRangeRequest::getMinAmount))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedRanges.size(); i++) {
            FeeRangeRequest current = sortedRanges.get(i);

            if (current.getMinAmount() == null || current.getMinAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Min amount must be provided and non-negative for all ranges");
            }

            if (current.getAmount() == null || current.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount must be provided and non-negative for all ranges");
            }

            if (i < sortedRanges.size() - 1) {
                FeeRangeRequest next = sortedRanges.get(i + 1);

                if (current.getMaxAmount() == null ||
                        !current.getMaxAmount().equals(next.getMinAmount())) {
                    throw new IllegalArgumentException("Ranges must be continuous without gaps or overlaps");
                }
            } else {
                if (current.getMaxAmount() != null && current.getMaxAmount().compareTo(current.getMinAmount()) <= 0) {
                    throw new IllegalArgumentException("Max amount must be greater than min amount for the last range");
                }
            }
        }

        log.debug("Fee ranges validation successful - Count: {} - Correlation ID: {}",
                feeRanges.size(), correlationId);
    }

    private void validateFeeSharers(List<FeeSharerRequest> feeSharers, String correlationId) {
        if (feeSharers == null || feeSharers.isEmpty()) {
            throw new IllegalArgumentException("Fee sharers must be provided when fee is shared");
        }

        if (feeSharers.size() < 2) {
            throw new IllegalArgumentException("At least 2 fee sharers are required (you + at least one other)");
        }

        // Validate that there's exactly one primary sharer
        long primaryCount = feeSharers.stream()
                .filter(FeeSharerRequest::getIsPrimary)
                .count();

        if (primaryCount != 1) {
            throw new IllegalArgumentException("There must be exactly one primary fee sharer");
        }

        // Validate total percentage equals 100
        BigDecimal totalPercentage = feeSharers.stream()
                .map(FeeSharerRequest::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercentage.compareTo(new BigDecimal("100")) != 0) {
            throw new IllegalArgumentException("Total percentage of all fee sharers must equal 100");
        }

        // Validate unique sharer IDs
        Set<String> sharerIds = feeSharers.stream()
                .map(FeeSharerRequest::getSharerId)
                .collect(Collectors.toSet());

        if (sharerIds.size() != feeSharers.size()) {
            throw new IllegalArgumentException("Fee sharer IDs must be unique");
        }

        log.debug("Fee sharers validation successful - Count: {} - Correlation ID: {}",
                feeSharers.size(), correlationId);
    }

    private void setCalculationSpecificFields(Fee fee, CreateFeeRequest request) {
        switch (request.getCalculationType()) {
            case FIXED:
                fee.setFixedAmount(request.getFixedAmount());
                break;
            case PERCENTAGE:
                fee.setPercentageRate(request.getPercentageRate());
                break;
            case RANGED:
                // Ranges are handled separately
                break;
        }
    }

    private void setFeeTypeSpecificReferences(Fee fee, CreateFeeRequest request) {
        switch (request.getFeeType()) {
            case PRODUCT:
                fee.setProductId(request.getProductId());
                break;
            case PROCESSOR:
                fee.setProcessorId(request.getProcessorId());
                break;
            case CLIENT:
                fee.setClientId(request.getClientId());
                break;
            case DEFAULT:
                // No specific references for default fees
                break;
        }
    }

    private void createFeeRanges(Fee fee, List<FeeRangeRequest> rangeRequests) {
        for (FeeRangeRequest rangeRequest : rangeRequests) {
            FeeRange feeRange = FeeRange.builder()
                    .fee(fee)
                    .minAmount(rangeRequest.getMinAmount())
                    .maxAmount(rangeRequest.getMaxAmount())
                    .amount(rangeRequest.getAmount())
                    .rate(rangeRequest.getRate())
                    .build();
            feeRangeRepository.save(feeRange);
        }
    }

    private void createFeeSharers(Fee fee, List<FeeSharerRequest> sharerRequests, String correlationId) {
        for (FeeSharerRequest sharerRequest : sharerRequests) {
            FeeSharer feeSharer = FeeSharer.builder()
                    .fee(fee)
                    .sharerType(sharerRequest.getSharerType())
                    .sharerId(sharerRequest.getSharerId())
                    .sharerName(sharerRequest.getSharerName())
                    .percentage(sharerRequest.getPercentage())
                    .isPrimary(sharerRequest.getIsPrimary())
                    .build();

            feeSharerRepository.save(feeSharer);
        }

        log.debug("Created {} fee sharers for fee ID: {} - Correlation ID: {}",
                sharerRequests.size(), fee.getId(), correlationId);
    }

    public List<Fee> getFeesByTenant(String tenantId) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(tenantId, null);
        Tenant tenant = getOrCreateTenant(effectiveTenantId, correlationId);

        log.debug("Fetching fees for tenant: {} - Correlation ID: {}", effectiveTenantId, correlationId);
        return feeRepository.findByTenant(tenant);
    }

    public Optional<Fee> getFeeByIdAndTenant(Long id, String tenantId) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(tenantId, null);
        Tenant tenant = getOrCreateTenant(effectiveTenantId, correlationId);

        log.debug("Fetching fee ID: {} for tenant: {} - Correlation ID: {}", id, effectiveTenantId, correlationId);
        return feeRepository.findByIdAndTenant(id, tenant);
    }

    public Fee activateFee(Long id, String tenantId) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(tenantId, null);
        Fee fee = getFeeByIdAndTenant(id, effectiveTenantId)
                .orElseThrow(() -> {
                    log.warn("Fee not found: {} for tenant: {} - Correlation ID: {}", id, effectiveTenantId, correlationId);
                    return new IllegalArgumentException("Fee not found");
                });

        fee.setIsActive(true);
        Fee updatedFee = feeRepository.save(fee);

        log.info("Fee activated: {} for tenant: {} - Correlation ID: {}", id, effectiveTenantId, correlationId);
        return updatedFee;
    }

    public Fee deactivateFee(Long id, String tenantId) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(tenantId, null);
        Fee fee = getFeeByIdAndTenant(id, effectiveTenantId)
                .orElseThrow(() -> {
                    log.warn("Fee not found: {} for tenant: {} - Correlation ID: {}", id, effectiveTenantId, correlationId);
                    return new IllegalArgumentException("Fee not found");
                });

        fee.setIsActive(false);
        Fee updatedFee = feeRepository.save(fee);

        log.info("Fee deactivated: {} for tenant: {} - Correlation ID: {}", id, effectiveTenantId, correlationId);
        return updatedFee;
    }

    public Fee updateFeeSharers(Long id, String tenantId, List<FeeSharerRequest> sharerRequests) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(tenantId, null);
        Fee fee = getFeeByIdAndTenant(id, effectiveTenantId)
                .orElseThrow(() -> {
                    log.warn("Fee not found: {} for tenant: {} - Correlation ID: {}", id, effectiveTenantId, correlationId);
                    return new IllegalArgumentException("Fee not found");
                });

        if (!fee.getIsShared()) {
            throw new IllegalArgumentException("Cannot update sharers for non-shared fee");
        }

        // Validate new sharers
        validateFeeSharers(sharerRequests, correlationId);

        // Remove existing sharers
        feeSharerRepository.deleteByFeeId(fee.getId());

        // Add new sharers
        createFeeSharers(fee, sharerRequests, correlationId);

        log.info("Updated fee sharers for fee ID: {} - Correlation ID: {}", id, correlationId);
        return fee;
    }

    public List<FeeSharer> getFeeSharers(Long feeId, String tenantId) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(tenantId, null);
        Fee fee = getFeeByIdAndTenant(feeId, effectiveTenantId)
                .orElseThrow(() -> {
                    log.warn("Fee not found: {} for tenant: {} - Correlation ID: {}", feeId, effectiveTenantId, correlationId);
                    return new IllegalArgumentException("Fee not found");
                });

        if (!fee.getIsShared()) {
            throw new IllegalArgumentException("Fee is not shared");
        }

        return feeSharerRepository.findByFeeId(feeId);
    }


    public Fee updateFee(Long id, String tenantId, UpdateFeeRequest request) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(tenantId, null);
        log.info("Updating fee ID: {} for tenant: {} - Correlation ID: {}", id, effectiveTenantId, correlationId);

        Tenant tenant = getOrCreateTenant(effectiveTenantId, correlationId);

        // Find existing fee
        Fee existingFee = feeRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> {
                    log.warn("Fee not found for update: {} for tenant: {} - Correlation ID: {}",
                            id, effectiveTenantId, correlationId);
                    return new IllegalArgumentException("Fee not found");
                });

        // Validate fee name uniqueness per tenant (excluding current fee)
        if (!existingFee.getName().equals(request.getName()) &&
                feeRepository.existsByTenantAndNameAndIdNot(tenant, request.getName(), id)) {
            log.warn("Fee name already exists: {} for tenant: {} - Correlation ID: {}",
                    request.getName(), effectiveTenantId, correlationId);
            throw new IllegalArgumentException("Fee with name '" + request.getName() + "' already exists for this tenant");
        }

        // Validate and get transaction type
        TransactionType transactionType = transactionTypeRepository
                .findByNameAndIsActive(request.getTransactionType(), true)
                .orElseThrow(() -> {
                    log.warn("Transaction type not found: {} for tenant: {} - Correlation ID: {}",
                            request.getTransactionType(), effectiveTenantId, correlationId);
                    return new IllegalArgumentException("Transaction type not found or inactive");
                });

        // Validate and get channels
        Set<Channel> channels = validateAndGetChannelsByName(tenant, request.getChannels(), correlationId);

        // Validate fee type specific requirements for update
        validateUpdateFeeRequest(request, existingFee.getFeeType(), existingFee.getCalculationType(), correlationId);

        // Validate fee sharers if fee is shared
        if (request.getIsShared() && request.getFeeSharers() != null) {
            validateFeeSharers(request.getFeeSharers(), correlationId);
        }

        // Update basic fields
        existingFee.setName(request.getName());
        existingFee.setDescription(request.getDescription());
        existingFee.setTransactionType(transactionType);
        existingFee.setIsShared(request.getIsShared());
        existingFee.setIsActive(request.getIsActive());

        // Update channels - clear existing and add new ones
        existingFee.getChannels().clear();
        channels.forEach(existingFee::addChannel);

        // Update calculation type specific fields
        updateCalculationSpecificFields(existingFee, request, existingFee.getCalculationType());

        // Update fee type specific references
        updateFeeTypeSpecificReferences(existingFee, request, existingFee.getFeeType());

        // Update fee ranges for RANGED calculation type
        if (existingFee.getCalculationType() == FeeCalculationType.RANGED) {
            updateFeeRanges(existingFee, request.getFeeRanges(), correlationId);
        }

        // Update fee sharers if fee is shared
        if (request.getIsShared()) {
            updateFeeSharers(existingFee, request.getFeeSharers(), correlationId);
        } else {
            // Remove all sharers if fee is no longer shared
            removeFeeSharers(existingFee, correlationId);
        }

        Fee updatedFee = feeRepository.save(existingFee);

        log.info("Fee updated successfully with ID: {} - Correlation ID: {}", updatedFee.getId(), correlationId);

        return updatedFee;
    }

    private void validateUpdateFeeRequest(UpdateFeeRequest request, FeeType existingFeeType,
                                          FeeCalculationType existingCalculationType, String correlationId) {
        log.debug("Validating fee update request - Existing Type: {}, Existing Calculation: {} - Correlation ID: {}",
                existingFeeType, existingCalculationType, correlationId);

        // If fee is shared, validate sharers
        if (request.getIsShared()) {
            if (request.getFeeSharers() == null || request.getFeeSharers().isEmpty()) {
                throw new IllegalArgumentException("Fee sharers must be provided when fee is shared");
            }
        }

        // If fee is not shared, but sharers are provided, throw error
        if (!request.getIsShared() && request.getFeeSharers() != null && !request.getFeeSharers().isEmpty()) {
            throw new IllegalArgumentException("Fee sharers should not be provided when fee is not shared");
        }

        // Validate calculation type specific fields based on existing calculation type
        switch (existingCalculationType) {
            case FIXED:
                if (request.getFixedAmount() == null || request.getFixedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Fixed amount must be provided and greater than 0 for FIXED fees");
                }
                break;
            case PERCENTAGE:
                if (request.getPercentageRate() == null || request.getPercentageRate().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Percentage rate must be provided and greater than 0 for PERCENTAGE fees");
                }
                break;
            case RANGED:
                if (request.getFeeRanges() == null || request.getFeeRanges().isEmpty()) {
                    throw new IllegalArgumentException("Fee ranges must be provided for RANGED fees");
                }
                validateFeeRanges(request.getFeeRanges(), correlationId);
                break;
        }

        // Validate fee type specific references based on existing fee type
        switch (existingFeeType) {
            case PRODUCT:
                if (request.getProductId() == null) {
                    throw new IllegalArgumentException("productId must be provided for PRODUCT fees");
                }
                break;
            case PROCESSOR:
                if (request.getProcessorId() == null) {
                    throw new IllegalArgumentException("processorId must be provided for PROCESSOR fees");
                }
                break;
            case CLIENT:
                if (request.getClientId() == null) {
                    throw new IllegalArgumentException("clientId must be provided for CLIENT fees");
                }
                break;
            case DEFAULT:
                // No specific references needed for default fees
                break;
        }
    }

    private void updateCalculationSpecificFields(Fee fee, UpdateFeeRequest request, FeeCalculationType calculationType) {
        // Clear previous calculation-specific fields
        fee.setFixedAmount(null);
        fee.setPercentageRate(null);

        switch (calculationType) {
            case FIXED:
                fee.setFixedAmount(request.getFixedAmount());
                break;
            case PERCENTAGE:
                fee.setPercentageRate(request.getPercentageRate());
                break;
            case RANGED:
                // Ranges are handled separately
                break;
        }
    }

    private void updateFeeTypeSpecificReferences(Fee fee, UpdateFeeRequest request, FeeType feeType) {
        // Clear previous fee type specific references
        fee.setProductId(null);
        fee.setProcessorId(null);
        fee.setClientId(null);

        switch (feeType) {
            case PRODUCT:
                fee.setProductId(request.getProductId());
                break;
            case PROCESSOR:
                fee.setProcessorId(request.getProcessorId());
                break;
            case CLIENT:
                fee.setClientId(request.getClientId());
                break;
            case DEFAULT:
                // No specific references for default fees
                break;
        }
    }

    private void updateFeeRanges(Fee fee, List<FeeRangeRequest> rangeRequests, String correlationId) {
        if (rangeRequests == null || rangeRequests.isEmpty()) {
            log.warn("No fee ranges provided for RANGED fee update - Correlation ID: {}", correlationId);
            throw new IllegalArgumentException("Fee ranges must be provided for RANGED fees");
        }

        // Remove existing ranges
        feeRangeRepository.deleteByFeeId(fee.getId());
        fee.getFeeRanges().clear();

        // Add new ranges
        for (FeeRangeRequest rangeRequest : rangeRequests) {
            FeeRange feeRange = FeeRange.builder()
                    .fee(fee)
                    .minAmount(rangeRequest.getMinAmount())
                    .maxAmount(rangeRequest.getMaxAmount())
                    .amount(rangeRequest.getAmount())
                    .rate(rangeRequest.getRate())
                    .build();

            feeRangeRepository.save(feeRange);
            fee.addFeeRange(feeRange);
        }

        log.debug("Updated {} fee ranges for fee ID: {} - Correlation ID: {}",
                rangeRequests.size(), fee.getId(), correlationId);
    }

    private void updateFeeSharers(Fee fee, List<FeeSharerRequest> sharerRequests, String correlationId) {
        if (sharerRequests == null || sharerRequests.isEmpty()) {
            log.warn("No fee sharers provided for shared fee update - Correlation ID: {}", correlationId);
            throw new IllegalArgumentException("Fee sharers must be provided when fee is shared");
        }

        // Remove existing sharers
        feeSharerRepository.deleteByFeeId(fee.getId());
        fee.getFeeSharers().clear();

        // Add new sharers
        for (FeeSharerRequest sharerRequest : sharerRequests) {
            FeeSharer feeSharer = FeeSharer.builder()
                    .fee(fee)
                    .sharerType(sharerRequest.getSharerType())
                    .sharerId(sharerRequest.getSharerId())
                    .sharerName(sharerRequest.getSharerName())
                    .percentage(sharerRequest.getPercentage())
                    .isPrimary(sharerRequest.getIsPrimary())
                    .build();

            feeSharerRepository.save(feeSharer);
            fee.addFeeSharer(feeSharer);
        }

        log.debug("Updated {} fee sharers for fee ID: {} - Correlation ID: {}",
                sharerRequests.size(), fee.getId(), correlationId);
    }

    private void removeFeeSharers(Fee fee, String correlationId) {
        if (!fee.getFeeSharers().isEmpty()) {
            feeSharerRepository.deleteByFeeId(fee.getId());
            fee.getFeeSharers().clear();
            log.debug("Removed all fee sharers for fee ID: {} - Correlation ID: {}", fee.getId(), correlationId);
        }
    }

    public Fee partialUpdateFee(Long id, String tenantId, Map<String, Object> updates) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(tenantId, null);
        log.info("Partial updating fee ID: {} for tenant: {} - Correlation ID: {}", id, effectiveTenantId, correlationId);

        Tenant tenant = getOrCreateTenant(effectiveTenantId, correlationId);

        // Find existing fee
        Fee existingFee = feeRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> {
                    log.warn("Fee not found for partial update: {} for tenant: {} - Correlation ID: {}",
                            id, effectiveTenantId, correlationId);
                    return new IllegalArgumentException("Fee not found");
                });

        // Apply partial updates
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            switch (field) {
                case "name":
                    String newName = (String) value;
                    if (!existingFee.getName().equals(newName) &&
                            feeRepository.existsByTenantAndNameAndIdNot(tenant, newName, id)) {
                        throw new IllegalArgumentException("Fee with name '" + newName + "' already exists for this tenant");
                    }
                    existingFee.setName(newName);
                    break;

                case "description":
                    existingFee.setDescription((String) value);
                    break;

                case "isActive":
                    existingFee.setIsActive((Boolean) value);
                    break;

                case "isShared":
                    Boolean newIsShared = (Boolean) value;
                    existingFee.setIsShared(newIsShared);
                    // If setting to not shared, remove all sharers
                    if (!newIsShared) {
                        removeFeeSharers(existingFee, correlationId);
                    }
                    break;

                case "fixedAmount":
                    if (existingFee.getCalculationType() != FeeCalculationType.FIXED) {
                        throw new IllegalArgumentException("Cannot update fixed amount for non-FIXED fee");
                    }
                    BigDecimal fixedAmount = new BigDecimal(value.toString());
                    if (fixedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Fixed amount must be greater than 0");
                    }
                    existingFee.setFixedAmount(fixedAmount);
                    break;

                case "percentageRate":
                    if (existingFee.getCalculationType() != FeeCalculationType.PERCENTAGE) {
                        throw new IllegalArgumentException("Cannot update percentage rate for non-PERCENTAGE fee");
                    }
                    BigDecimal percentageRate = new BigDecimal(value.toString());
                    if (percentageRate.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Percentage rate must be greater than 0");
                    }
                    existingFee.setPercentageRate(percentageRate);
                    break;

                default:
                    log.warn("Unknown field for partial update: {} - Correlation ID: {}", field, correlationId);
                    throw new IllegalArgumentException("Unknown field: " + field);
            }
        }

        Fee updatedFee = feeRepository.save(existingFee);

        log.info("Fee partially updated successfully with ID: {} - Correlation ID: {}", updatedFee.getId(), correlationId);

        return updatedFee;
    }

    public FeeCalculationResult calculateFee(FeeCalculationRequest request) {
        String correlationId = CorrelationIdContext.getCorrelationId();

        String effectiveTenantId = getEffectiveTenantId(request.getTenantId(), null);

        // Log the cascade search criteria
        StringBuilder cascadeInfo = new StringBuilder();
        cascadeInfo.append("Cascade search: ");
        if (request.getClientId() != null) cascadeInfo.append("CLIENT(").append(request.getClientId()).append(") → ");
        if (request.getProductId() != null) cascadeInfo.append("PRODUCT(").append(request.getProductId()).append(") → ");
        if (request.getProcessorId() != null) cascadeInfo.append("PROCESSOR(").append(request.getProcessorId()).append(") → ");
        cascadeInfo.append("DEFAULT");

        log.info("Calculating fee for tenant: {}, amount: {} - {} - Correlation ID: {}",
                effectiveTenantId, request.getAmount(), cascadeInfo.toString(), correlationId);

        Tenant tenant = getOrCreateTenant(effectiveTenantId, correlationId);

        // Validate and get transaction type
        TransactionType transactionType = transactionTypeRepository
                .findByNameAndIsActive(request.getTransactionType(), true)
                .orElseThrow(() -> {
                    log.warn("Transaction type not found: {} - Correlation ID: {}",
                            request.getTransactionType(), correlationId);
                    return new IllegalArgumentException("Transaction type not found or inactive");
                });

        // Validate and get channel
        Channel channel = channelRepository
                .findByNameAndIsActive(request.getChannel(), true)
                .orElseThrow(() -> {
                    log.warn("Channel not found: {} - Correlation ID: {}", request.getChannel(), correlationId);
                    return new IllegalArgumentException("Channel not found or inactive");
                });

        // Find applicable fee with cascading logic
        Fee applicableFee = findApplicableFeeWithCascading(tenant, request, transactionType, channel, correlationId);

        if (applicableFee == null) {
            log.warn("No applicable fee found in cascade - Correlation ID: {}", correlationId);
            throw new IllegalArgumentException("No applicable fee found for the given criteria");
        }

        // Calculate fee amount
        BigDecimal calculatedAmount = calculateFeeAmount(applicableFee, request.getAmount(), correlationId);

        // Calculate sharer distributions if applicable
        List<FeeSharerCalculation> sharerCalculations = new ArrayList<>();
        if (request.getApplySharing() && applicableFee.getIsShared() && !applicableFee.getFeeSharers().isEmpty()) {
            sharerCalculations = calculateSharerDistributions(applicableFee, calculatedAmount, correlationId);
        }

        log.info("Fee calculation completed - Amount: {}, Fee: {}, Total: {}, Applied Fee Type: {} - Correlation ID: {}",
                request.getAmount(), calculatedAmount, request.getAmount().add(calculatedAmount),
                applicableFee.getFeeType(), correlationId);

        return FeeCalculationResult.builder()
                .fee(applicableFee)
                .calculatedAmount(calculatedAmount)
                .sharerCalculations(sharerCalculations)
                .build();
    }

    private Fee findApplicableFeeWithCascading(Tenant tenant, FeeCalculationRequest request,
                                               TransactionType transactionType, Channel channel, String correlationId) {
        log.debug("Finding applicable fee with cascading logic - Correlation ID: {}", correlationId);

        // Get all active fees for the tenant that match transaction type and channel
        List<Fee> allFees = feeRepository.findByTenantAndIsActive(tenant, true);

        List<Fee> matchingFees = allFees.stream()
                .filter(fee -> fee.getTransactionType().getId().equals(transactionType.getId()))
                .filter(fee -> fee.getChannels().contains(channel))
                .collect(Collectors.toList());

        log.debug("Found {} fees matching transaction type and channel - Correlation ID: {}",
                matchingFees.size(), correlationId);

        // Cascading logic: CLIENT → PRODUCT → PROCESSOR → DEFAULT
        Fee applicableFee = null;

        // 1. CLIENT (Highest priority)
        if (request.getClientId() != null && !request.getClientId().trim().isEmpty()) {
            applicableFee = findMatchingFee(matchingFees, FeeType.CLIENT, null, null, request.getClientId(), correlationId);
            if (applicableFee != null) {
                log.debug("Found CLIENT fee - Correlation ID: {}", correlationId);
                return applicableFee;
            }
            log.debug("No CLIENT fee found for clientId: {} - Continuing cascade - Correlation ID: {}",
                    request.getClientId(), correlationId);
        }

        // 2. PRODUCT (Second priority)
        if (request.getProductId() != null && !request.getProductId().trim().isEmpty()) {
            applicableFee = findMatchingFee(matchingFees, FeeType.PRODUCT, request.getProductId(), null, null, correlationId);
            if (applicableFee != null) {
                log.debug("Found PRODUCT fee - Correlation ID: {}", correlationId);
                return applicableFee;
            }
            log.debug("No PRODUCT fee found for productId: {} - Continuing cascade - Correlation ID: {}",
                    request.getProductId(), correlationId);
        }

        // 3. PROCESSOR (Third priority)
        if (request.getProcessorId() != null && !request.getProcessorId().trim().isEmpty()) {
            applicableFee = findMatchingFee(matchingFees, FeeType.PROCESSOR, null, request.getProcessorId(), null, correlationId);
            if (applicableFee != null) {
                log.debug("Found PROCESSOR fee - Correlation ID: {}", correlationId);
                return applicableFee;
            }
            log.debug("No PROCESSOR fee found for processorId: {} - Continuing cascade - Correlation ID: {}",
                    request.getProcessorId(), correlationId);
        }

        // 4. DEFAULT (Lowest priority - fallback)
        applicableFee = findMatchingFee(matchingFees, FeeType.DEFAULT, null, null, null, correlationId);
        if (applicableFee != null) {
            log.debug("Found DEFAULT fee - Correlation ID: {}", correlationId);
            return applicableFee;
        }

        log.debug("No applicable fee found in cascading search - Correlation ID: {}", correlationId);
        return null;
    }

    private Fee findMatchingFee(List<Fee> fees, FeeType feeType, String productId, String processorId, String clientId, String correlationId) {
        List<Fee> typedFees = fees.stream()
                .filter(fee -> fee.getFeeType() == feeType)
                .collect(Collectors.toList());

        log.debug("Searching for {} fee - Available: {} - Correlation ID: {}", feeType, typedFees.size(), correlationId);

        for (Fee fee : typedFees) {
            boolean matches = false;

            switch (feeType) {
                case PRODUCT:
                    matches = fee.getProductId() != null && fee.getProductId().equals(productId);
                    break;
                case PROCESSOR:
                    matches = fee.getProcessorId() != null && fee.getProcessorId().equals(processorId);
                    break;
                case CLIENT:
                    matches = fee.getClientId() != null && fee.getClientId().equals(clientId);
                    break;
                case DEFAULT:
                    matches = true; // DEFAULT fees don't have specific IDs
                    break;
            }

            if (matches) {
                log.debug("Found matching {} fee: {} - Correlation ID: {}", feeType, fee.getId(), correlationId);
                return fee;
            }
        }

        log.debug("No matching {} fee found - Correlation ID: {}", feeType, correlationId);
        return null;
    }

    private BigDecimal calculateFeeAmount(Fee fee, BigDecimal amount, String correlationId) {
        log.debug("Calculating fee amount for fee ID: {}, amount: {}, type: {} - Correlation ID: {}",
                fee.getId(), amount, fee.getCalculationType(), correlationId);

        BigDecimal calculatedAmount;

        switch (fee.getCalculationType()) {
            case FIXED:
                calculatedAmount = calculateFixedFee(fee, amount);
                break;
            case PERCENTAGE:
                calculatedAmount = calculatePercentageFee(fee, amount);
                break;
            case RANGED:
                calculatedAmount = calculateRangedFee(fee, amount, correlationId);
                break;
            default:
                throw new IllegalArgumentException("Unsupported calculation type: " + fee.getCalculationType());
        }

        // Ensure fee amount is non-negative
        if (calculatedAmount.compareTo(BigDecimal.ZERO) < 0) {
            calculatedAmount = BigDecimal.ZERO;
        }

        log.debug("Calculated fee amount: {} - Correlation ID: {}", calculatedAmount, correlationId);
        return calculatedAmount;
    }

    private BigDecimal calculateFixedFee(Fee fee, BigDecimal amount) {
        return fee.getFixedAmount();
    }

    private BigDecimal calculatePercentageFee(Fee fee, BigDecimal amount) {
        return amount.multiply(fee.getPercentageRate().divide(new BigDecimal("100")));
    }

    private BigDecimal calculateRangedFee(Fee fee, BigDecimal amount, String correlationId) {
        if (fee.getFeeRanges() == null || fee.getFeeRanges().isEmpty()) {
            log.warn("No ranges defined for RANGED fee ID: {} - Correlation ID: {}", fee.getId(), correlationId);
            throw new IllegalArgumentException("No ranges defined for RANGED fee");
        }

        // Sort ranges by min amount
        List<FeeRange> sortedRanges = fee.getFeeRanges().stream()
                .sorted(Comparator.comparing(FeeRange::getMinAmount))
                .collect(Collectors.toList());

        // Find applicable range
        for (FeeRange range : sortedRanges) {
            if (amount.compareTo(range.getMinAmount()) >= 0) {
                // Check if amount is within this range (or if it's the last range with no max)
                if (range.getMaxAmount() == null || amount.compareTo(range.getMaxAmount()) < 0) {
                    log.debug("Using range: {} - {} for amount: {} - Correlation ID: {}",
                            range.getMinAmount(), range.getMaxAmount(), amount, correlationId);

                    // Calculate based on amount or rate
                    if (range.getRate() != null && range.getRate().compareTo(BigDecimal.ZERO) > 0) {
                        // Percentage-based range
                        return amount.multiply(range.getRate().divide(new BigDecimal("100")));
                    } else {
                        // Fixed amount range
                        return range.getAmount();
                    }
                }
            }
        }

        // If no range found, use the last range (should handle infinite upper bound)
        FeeRange lastRange = sortedRanges.get(sortedRanges.size() - 1);
        if (lastRange.getMaxAmount() == null) {
            log.debug("Using last range (infinite) for amount: {} - Correlation ID: {}", amount, correlationId);
            if (lastRange.getRate() != null && lastRange.getRate().compareTo(BigDecimal.ZERO) > 0) {
                return amount.multiply(lastRange.getRate().divide(new BigDecimal("100")));
            } else {
                return lastRange.getAmount();
            }
        }

        log.warn("No applicable range found for amount: {} in fee ID: {} - Correlation ID: {}",
                amount, fee.getId(), correlationId);
        throw new IllegalArgumentException("No applicable range found for amount: " + amount);
    }

    private List<FeeSharerCalculation> calculateSharerDistributions(Fee fee, BigDecimal totalFeeAmount, String correlationId) {
        log.debug("Calculating sharer distributions for fee ID: {}, total amount: {} - Correlation ID: {}",
                fee.getId(), totalFeeAmount, correlationId);

        List<FeeSharerCalculation> calculations = new ArrayList<>();

        for (FeeSharer sharer : fee.getFeeSharers()) {
            BigDecimal sharerAmount = totalFeeAmount.multiply(sharer.getPercentage().divide(new BigDecimal("100")));

            FeeSharerCalculation calculation = FeeSharerCalculation.builder()
                    .sharerType(sharer.getSharerType())
                    .sharerId(sharer.getSharerId())
                    .sharerName(sharer.getSharerName())
                    .percentage(sharer.getPercentage())
                    .amount(sharerAmount)
                    .isPrimary(sharer.getIsPrimary())
                    .build();

            calculations.add(calculation);
        }

        // Validate total distribution equals total fee amount (with rounding tolerance)
        BigDecimal distributedTotal = calculations.stream()
                .map(FeeSharerCalculation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal difference = totalFeeAmount.subtract(distributedTotal).abs();
        if (difference.compareTo(new BigDecimal("0.01")) > 0) {
            log.warn("Sharer distribution mismatch - Total: {}, Distributed: {} - Correlation ID: {}",
                    totalFeeAmount, distributedTotal, correlationId);
            // Adjust the primary sharer's amount to account for rounding differences
            adjustPrimarySharerAmount(calculations, totalFeeAmount, distributedTotal);
        }

        log.debug("Calculated distributions for {} sharers - Correlation ID: {}", calculations.size(), correlationId);
        return calculations;
    }

    private void adjustPrimarySharerAmount(List<FeeSharerCalculation> calculations, BigDecimal totalFeeAmount, BigDecimal distributedTotal) {
        Optional<FeeSharerCalculation> primarySharer = calculations.stream()
                .filter(FeeSharerCalculation::getIsPrimary)
                .findFirst();

        if (primarySharer.isPresent()) {
            BigDecimal adjustment = totalFeeAmount.subtract(distributedTotal);
            FeeSharerCalculation primary = primarySharer.get();
            primary.setAmount(primary.getAmount().add(adjustment));

            log.debug("Adjusted primary sharer amount by: {}", adjustment);
        }
    }
}