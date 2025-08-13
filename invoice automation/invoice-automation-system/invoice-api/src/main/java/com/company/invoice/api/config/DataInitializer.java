package com.company.invoice.api.config;

import com.company.invoice.api.service.AuthenticationService;
import com.company.invoice.data.entity.InvoicePattern;
import com.company.invoice.data.repository.InvoicePatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Initializes default data when the application starts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AuthenticationService authenticationService;
    private final InvoicePatternRepository patternRepository;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("🚀 Initializing default data...");
            
            // Create default users with different roles
            authenticationService.createDefaultUsers();
            
            // Initialize golden extraction patterns
            initializeGoldenPatterns();
            
            log.info("✅ Data initialization completed successfully!");
            
        } catch (Exception e) {
            log.error("❌ Error during data initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize golden patterns for invoice data extraction
     * Based on real invoice analysis from user-provided samples
     */
    private void initializeGoldenPatterns() {
        log.info("🎯 Initializing golden extraction patterns...");
        
        try {
            // Check if patterns already exist to avoid duplicates
            long existingPatterns = patternRepository.count();
            if (existingPatterns > 0) {
                log.info("📋 Patterns already exist ({}), skipping initialization", existingPatterns);
                return;
            }
            
            List<InvoicePattern> goldenPatterns = List.of(
                // ==== TOTAL AMOUNT PATTERNS ====
                
                // Pattern 1: US Format - "Total: $1,146.48"
                InvoicePattern.builder()
                    .patternName("US Total with Dollar Sign")
                    .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                    .patternRegex("(?i)\\btotal\\s*:?\\s*\\$([0-9,]+\\.\\d{2})")
                    .patternDescription("Matches US format totals with dollar sign prefix: Total: $1,146.48")
                    .patternPriority(10)
                    .confidenceWeight(new BigDecimal("0.95"))
                    .captureGroup(1)
                    .notes("Based on SuperStore invoice samples - captures US currency format")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // Pattern 2: European Format - "Total 381,12" or "Gross Amount incl. VAT 453,53"
                InvoicePattern.builder()
                    .patternName("European Total with Comma Decimal")
                    .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                    .patternRegex("(?i)(?:total|gross\\s+amount[^0-9]*?)\\s*([0-9]{1,3}(?:\\.[0-9]{3})*,\\d{2})")
                    .patternDescription("Matches European format totals with comma decimal: Total 381,12")
                    .patternPriority(15)
                    .confidenceWeight(new BigDecimal("0.90"))
                    .captureGroup(1)
                    .notes("Based on CPB Software German invoice - European number format")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // Pattern 3: Balance Due Format - "Balance Due: $9,118.21"
                InvoicePattern.builder()
                    .patternName("Balance Due Amount")
                    .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                    .patternRegex("(?i)\\bbalance\\s+due\\s*:?\\s*\\$([0-9,]+\\.\\d{2})")
                    .patternDescription("Matches balance due amounts: Balance Due: $9,118.21")
                    .patternPriority(20)
                    .confidenceWeight(new BigDecimal("0.92"))
                    .captureGroup(1)
                    .notes("Based on SuperStore invoices - balance due field")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // Pattern 4: Generic Amount Pattern - "Amount 130,00"
                InvoicePattern.builder()
                    .patternName("Generic Amount Field")
                    .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                    .patternRegex("(?i)\\bamount[^0-9]*?([0-9,]+(?:\\.[0-9]{2}|,\\d{2}))")
                    .patternDescription("Matches generic amount fields with various formats")
                    .patternPriority(30)
                    .confidenceWeight(new BigDecimal("0.80"))
                    .captureGroup(1)
                    .notes("Flexible pattern for various amount field formats")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // Pattern 5: End-of-line Total Pattern
                InvoicePattern.builder()
                    .patternName("End of Line Total")
                    .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                    .patternRegex("(?i)\\btotal\\s*:?\\s*([0-9,]+(?:\\.[0-9]{2}|,\\d{2}))\\s*$")
                    .patternDescription("Matches total amounts at end of line")
                    .patternPriority(25)
                    .confidenceWeight(new BigDecimal("0.85"))
                    .captureGroup(1)
                    .notes("Catches totals that appear at the end of lines")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // ==== INVOICE NUMBER PATTERNS ====
                
                // Pattern 6: Invoice Number with # - "INVOICE # 17042"
                InvoicePattern.builder()
                    .patternName("Invoice Number with Hash")
                    .patternCategory(InvoicePattern.PatternCategory.INVOICE_NUMBER)
                    .patternRegex("(?i)\\binvoice\\s*#\\s*([0-9]+)")
                    .patternDescription("Matches invoice numbers with hash symbol: INVOICE # 17042")
                    .patternPriority(10)
                    .confidenceWeight(new BigDecimal("0.95"))
                    .captureGroup(1)
                    .notes("Based on SuperStore invoice format")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // Pattern 7: Invoice No Format - "Invoice No 123100401"
                InvoicePattern.builder()
                    .patternName("Invoice No Format")
                    .patternCategory(InvoicePattern.PatternCategory.INVOICE_NUMBER)
                    .patternRegex("(?i)\\binvoice\\s+no\\.?\\s*([0-9]+)")
                    .patternDescription("Matches invoice numbers with 'No': Invoice No 123100401")
                    .patternPriority(15)
                    .confidenceWeight(new BigDecimal("0.90"))
                    .captureGroup(1)
                    .notes("Based on CPB Software invoice format")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // ==== DATE PATTERNS ====
                
                // Pattern 8: Date Format - "Nov 23 2012"
                InvoicePattern.builder()
                    .patternName("Month Day Year Format")
                    .patternCategory(InvoicePattern.PatternCategory.DATE)
                    .patternRegex("(?i)\\b((?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\s+\\d{1,2}\\s+\\d{4})\\b")
                    .patternDescription("Matches dates in 'Nov 23 2012' format")
                    .patternPriority(10)
                    .confidenceWeight(new BigDecimal("0.85"))
                    .captureGroup(1)
                    .dateFormat("MMM dd yyyy")
                    .notes("Based on SuperStore invoice date format")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // Pattern 9: European Date Format - "1. März 2024"
                InvoicePattern.builder()
                    .patternName("German Date Format")
                    .patternCategory(InvoicePattern.PatternCategory.DATE)
                    .patternRegex("(?i)\\b(\\d{1,2}\\.\\s*(?:januar|februar|märz|april|mai|juni|juli|august|september|oktober|november|dezember)\\s+\\d{4})\\b")
                    .patternDescription("Matches German date format: 1. März 2024")
                    .patternPriority(15)
                    .confidenceWeight(new BigDecimal("0.88"))
                    .captureGroup(1)
                    .dateFormat("d. MMMM yyyy")
                    .notes("Based on CPB Software German invoice")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // Pattern 10: Numeric Date Format - "01.02.2024"
                InvoicePattern.builder()
                    .patternName("European Numeric Date")
                    .patternCategory(InvoicePattern.PatternCategory.DATE)
                    .patternRegex("\\b(\\d{2}\\.\\d{2}\\.\\d{4})\\b")
                    .patternDescription("Matches European numeric dates: 01.02.2024")
                    .patternPriority(20)
                    .confidenceWeight(new BigDecimal("0.75"))
                    .captureGroup(1)
                    .dateFormat("dd.MM.yyyy")
                    .notes("Common European date format")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // ==== VENDOR PATTERNS ====
                
                // Pattern 11: Company Name with GmbH
                InvoicePattern.builder()
                    .patternName("German Company Name")
                    .patternCategory(InvoicePattern.PatternCategory.VENDOR)
                    .patternRegex("^([A-Za-z\\s&]+(?:GmbH|AG|KG))\\s*-")
                    .patternDescription("Matches German company names: CPB Software (Germany) GmbH")
                    .patternPriority(15)
                    .confidenceWeight(new BigDecimal("0.85"))
                    .captureGroup(1)
                    .notes("Based on CPB Software invoice header")
                    .createdBy("SYSTEM_GOLDEN")
                    .build(),
                
                // Pattern 12: Simple Company Name
                InvoicePattern.builder()
                    .patternName("Simple Company Name")
                    .patternCategory(InvoicePattern.PatternCategory.VENDOR)
                    .patternRegex("^([A-Za-z\\s&]+(?:Store|Corp|Inc|Ltd|LLC))\\b")
                    .patternDescription("Matches simple company names: SuperStore")
                    .patternPriority(20)
                    .confidenceWeight(new BigDecimal("0.80"))
                    .captureGroup(1)
                    .notes("Based on SuperStore invoices")
                    .createdBy("SYSTEM_GOLDEN")
                    .build()
            );
            
            // Save all patterns
            List<InvoicePattern> savedPatterns = patternRepository.saveAll(goldenPatterns);
            
            log.info("✅ Successfully created {} golden extraction patterns", savedPatterns.size());
            
            // Log pattern summary by category
            goldenPatterns.stream()
                .collect(java.util.stream.Collectors.groupingBy(InvoicePattern::getPatternCategory))
                .forEach((category, patterns) -> 
                    log.info("📊 Category {}: {} patterns", category, patterns.size()));
                    
        } catch (Exception e) {
            log.error("❌ Error initializing golden patterns: {}", e.getMessage(), e);
        }
    }
}
