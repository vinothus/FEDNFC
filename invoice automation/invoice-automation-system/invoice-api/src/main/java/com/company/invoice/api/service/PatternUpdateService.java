package com.company.invoice.api.service;

import com.company.invoice.data.entity.InvoicePattern;
import com.company.invoice.data.repository.InvoicePatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service to update and add new invoice patterns without requiring full re-seeding
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatternUpdateService {

    private final InvoicePatternRepository patternRepository;

    /**
     * Add missing high-priority pattern for "Total: $amount" format
     */
    @Transactional
    public void addMissingTotalColonPattern() {
        String patternName = "Total_Colon_HighPriority";
        
        // Check if this specific pattern already exists
        Optional<InvoicePattern> existing = patternRepository.findByPatternNameIgnoreCase(patternName);
        if (existing.isPresent()) {
            log.info("🔍 Pattern '{}' already exists, skipping", patternName);
            return;
        }

        log.info("🆕 Adding missing high-priority pattern for 'Total: $amount' format");

        InvoicePattern newPattern = InvoicePattern.builder()
                .patternName(patternName)
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?i)\\btotal\\s*:\\s*\\$([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternDescription("Total: $6,590.50 - High priority pattern for colon format with dollar sign")
                .patternPriority(7) // Higher priority than the existing colon pattern (priority 20)
                .confidenceWeight(BigDecimal.valueOf(0.98))
                .captureGroup(1)
                .isActive(true)
                .notes("Added for SuperStore invoice format - handles 'Total: $amount' with high priority")
                .createdBy("SYSTEM_UPDATE")
                .createdAt(LocalDateTime.now())
                .build();

        InvoicePattern saved = patternRepository.save(newPattern);
        log.info("✅ Successfully added pattern: {} (ID: {})", saved.getPatternName(), saved.getId());
        
        // Log the pattern details for verification
        log.info("📋 Pattern Details: Regex='{}', Priority={}, Confidence={}", 
                saved.getPatternRegex(), saved.getPatternPriority(), saved.getConfidenceWeight());
    }

    /**
     * Add pattern for subtotal with colon format
     */
    @Transactional
    public void addMissingSubtotalColonPattern() {
        String patternName = "Subtotal_Colon_HighPriority";
        
        Optional<InvoicePattern> existing = patternRepository.findByPatternNameIgnoreCase(patternName);
        if (existing.isPresent()) {
            log.info("🔍 Pattern '{}' already exists, skipping", patternName);
            return;
        }

        log.info("🆕 Adding missing high-priority pattern for 'Subtotal: $amount' format");

        InvoicePattern newPattern = InvoicePattern.builder()
                .patternName(patternName)
                .patternCategory(InvoicePattern.PatternCategory.SUBTOTAL_AMOUNT)
                .patternRegex("(?i)\\bsubtotal\\s*:\\s*\\$([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternDescription("Subtotal: $6,522.39 - High priority pattern for colon format")
                .patternPriority(7)
                .confidenceWeight(BigDecimal.valueOf(0.96))
                .captureGroup(1)
                .isActive(true)
                .notes("Added for SuperStore invoice format - handles 'Subtotal: $amount'")
                .createdBy("SYSTEM_UPDATE")
                .createdAt(LocalDateTime.now())
                .build();

        patternRepository.save(newPattern);
        log.info("✅ Successfully added subtotal pattern: {}", newPattern.getPatternName());
    }

    /**
     * Add pattern for shipping with colon format
     */
    @Transactional
    public void addMissingShippingColonPattern() {
        String patternName = "Shipping_Colon_HighPriority";
        
        Optional<InvoicePattern> existing = patternRepository.findByPatternNameIgnoreCase(patternName);
        if (existing.isPresent()) {
            log.info("🔍 Pattern '{}' already exists, skipping", patternName);
            return;
        }

        log.info("🆕 Adding missing high-priority pattern for 'Shipping: $amount' format");

        InvoicePattern newPattern = InvoicePattern.builder()
                .patternName(patternName)
                .patternCategory(InvoicePattern.PatternCategory.TAX_AMOUNT) // Using TAX_AMOUNT for shipping
                .patternRegex("(?i)\\bshipping\\s*:\\s*\\$([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternDescription("Shipping: $68.11 - High priority pattern for shipping costs")
                .patternPriority(8)
                .confidenceWeight(BigDecimal.valueOf(0.92))
                .captureGroup(1)
                .isActive(true)
                .notes("Added for SuperStore invoice format - handles 'Shipping: $amount'")
                .createdBy("SYSTEM_UPDATE")
                .createdAt(LocalDateTime.now())
                .build();

        patternRepository.save(newPattern);
        log.info("✅ Successfully added shipping pattern: {}", newPattern.getPatternName());
    }

    /**
     * Update all missing patterns for SuperStore format
     */
    @Transactional
    public void updatePatternsForSuperStore() {
        log.info("🔧 Updating patterns for SuperStore invoice format...");
        
        addMissingTotalColonPattern();
        addMissingSubtotalColonPattern(); 
        addMissingShippingColonPattern();
        
        // Get updated count
        long totalPatterns = patternRepository.count();
        long amountPatterns = patternRepository.countActivePatternssByCategory(
                InvoicePattern.PatternCategory.AMOUNT);
        
        log.info("✅ Pattern update completed! Total patterns: {}, Amount patterns: {}", 
                totalPatterns, amountPatterns);
    }
}
