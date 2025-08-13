package com.company.invoice.email.service;

import com.company.invoice.data.entity.InvoicePattern;
import com.company.invoice.data.repository.InvoicePatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to seed the database with golden invoice patterns.
 * This runs on application startup to ensure essential patterns are available.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatternDataSeeder implements CommandLineRunner {

    private final InvoicePatternRepository patternRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (patternRepository.count() == 0) {
            log.info("🌱 Seeding database with golden invoice patterns...");
            seedGoldenPatterns();
            log.info("✅ Golden patterns seeded successfully!");
        } else {
            log.info("📋 Invoice patterns already exist in database, skipping seeding");
        }
    }

    private void seedGoldenPatterns() {
        List<InvoicePattern> patterns = new ArrayList<>();

        // Add your specific invoice patterns first (highest priority)
        patterns.addAll(createYourInvoicePatterns());
        
        // Add general patterns (lower priority)
        patterns.addAll(createGeneralInvoicePatterns());
        
        // Add date patterns
        patterns.addAll(createDatePatterns());
        
        // Add vendor and customer patterns
        patterns.addAll(createVendorAndCustomerPatterns());
        
        // Add specialized patterns
        patterns.addAll(createSpecializedPatterns());
        
        // Add patterns for failed invoice formats
        patterns.addAll(createFailedInvoicePatterns());

        // Save all patterns
        List<InvoicePattern> savedPatterns = patternRepository.saveAll(patterns);
        log.info("💾 Saved {} golden patterns to database", savedPatterns.size());

        // Log pattern summary
        logPatternSummary(savedPatterns);
    }

    /**
     * Patterns specifically for your invoice format (tabular, no colons)
     */
    private List<InvoicePattern> createYourInvoicePatterns() {
        List<InvoicePattern> patterns = new ArrayList<>();

        // Invoice Number - Your format: "Invoice Number INV-3337"
        patterns.add(InvoicePattern.builder()
                .patternName("InvoiceNumber_YourFormat")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_NUMBER)
                .patternRegex("(?i)invoice\\s+number\\s+([A-Z0-9-]{3,})")
                .patternPriority(5)
                .confidenceWeight(BigDecimal.valueOf(1.0))
                .patternDescription("Invoice Number INV-3337 (your tabular format)")
                .notes("Highest priority for user's specific invoice format")
                .build());

        // Order Number as fallback for Invoice Number
        patterns.add(InvoicePattern.builder()
                .patternName("OrderNumber_AsInvoiceNumber")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_NUMBER)
                .patternRegex("(?i)order\\s+number\\s+([A-Z0-9-]{3,})")
                .patternPriority(8)
                .confidenceWeight(BigDecimal.valueOf(0.8))
                .patternDescription("Order Number 12345 (as invoice reference)")
                .notes("User's invoice has order number - use as fallback")
                .build());

        // Total Due - Your format: "Total Due $93.50"
        patterns.add(InvoicePattern.builder()
                .patternName("TotalDue_YourFormat")
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?i)total\\s+due\\s+(?:\\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternPriority(5)
                .confidenceWeight(BigDecimal.valueOf(1.0))
                .patternDescription("Total Due $93.50 (your primary total)")
                .notes("Highest priority amount pattern for user's format")
                .build());

        // Final Total - Your format: "Total $93.50" at end
        patterns.add(InvoicePattern.builder()
                .patternName("Total_EndOfLine_YourFormat")
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?i)^total\\s+(?:\\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)\\s*$")
                .patternFlags(2 | 8) // CASE_INSENSITIVE | MULTILINE
                .patternPriority(6)
                .confidenceWeight(BigDecimal.valueOf(0.95))
                .patternDescription("Total $93.50 (end of line)")
                .notes("Final total at bottom of invoice")
                .build());

        // Sub Total - Your format: "Sub Total $85.00"
        patterns.add(InvoicePattern.builder()
                .patternName("SubTotal_YourFormat")
                .patternCategory(InvoicePattern.PatternCategory.SUBTOTAL_AMOUNT)
                .patternRegex("(?i)sub\\s+total\\s+(?:\\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternPriority(5)
                .confidenceWeight(BigDecimal.valueOf(0.9))
                .patternDescription("Sub Total $85.00")
                .notes("Subtotal for user's format")
                .build());

        // Tax - Your format: "Tax $8.50"
        patterns.add(InvoicePattern.builder()
                .patternName("Tax_YourFormat")
                .patternCategory(InvoicePattern.PatternCategory.TAX_AMOUNT)
                .patternRegex("(?i)tax\\s+(?:\\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternPriority(5)
                .confidenceWeight(BigDecimal.valueOf(0.9))
                .patternDescription("Tax $8.50")
                .notes("Tax amount for user's format")
                .build());

        // Vendor - Your format: "From: DEMO - Sliced Invoices"
        patterns.add(InvoicePattern.builder()
                .patternName("Vendor_From_YourFormat")
                .patternCategory(InvoicePattern.PatternCategory.VENDOR)
                .patternRegex("(?i)from:\\s*([A-Za-z0-9\\s\\-,.&]+?)(?=\\s*(?:suite|street|avenue|road|po\\s+box|\\d+|$))")
                .patternPriority(5)
                .confidenceWeight(BigDecimal.valueOf(1.0))
                .patternDescription("From: DEMO - Sliced Invoices")
                .notes("Vendor extraction for user's format")
                .build());

        return patterns;
    }

    /**
     * General invoice patterns (traditional formats)
     */
    private List<InvoicePattern> createGeneralInvoicePatterns() {
        List<InvoicePattern> patterns = new ArrayList<>();

        // Traditional Invoice Number patterns
        patterns.add(InvoicePattern.builder()
                .patternName("InvoiceNumber_WithColon")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_NUMBER)
                .patternRegex("(?i)invoice\\s+number\\s*:\\s*([A-Z0-9-]{3,})")
                .patternPriority(15)
                .confidenceWeight(BigDecimal.valueOf(0.9))
                .patternDescription("Invoice Number: INV-3337")
                .notes("Traditional colon format")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("InvoiceNumber_Hash")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_NUMBER)
                .patternRegex("(?i)(?:invoice|inv)\\s*[-#:]?\\s*([A-Z0-9]{3,}-?[0-9]{3,})")
                .patternPriority(20)
                .confidenceWeight(BigDecimal.valueOf(0.8))
                .patternDescription("Invoice #123456 or INV-123456")
                .notes("Hash or dash format")
                .build());

        // Traditional Amount patterns
        patterns.add(InvoicePattern.builder()
                .patternName("Amount_WithColon")
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?i)(?:total|amount|sum)\\s*:\\s*(?:\\$|USD|€|EUR|£|GBP)\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternPriority(20)
                .confidenceWeight(BigDecimal.valueOf(0.8))
                .patternDescription("Total: $93.50")
                .notes("Traditional colon format")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("Amount_CurrencyFirst")
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?:\\$|USD|€|EUR|£|GBP)\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternPriority(30)
                .confidenceWeight(BigDecimal.valueOf(0.7))
                .patternDescription("$1,234.56 or USD 1,234.56")
                .notes("Currency symbol first")
                .build());

        return patterns;
    }

    /**
     * Date patterns for your format and common alternatives
     */
    private List<InvoicePattern> createDatePatterns() {
        List<InvoicePattern> patterns = new ArrayList<>();

        // Your format: "January 25, 2016"
        patterns.add(InvoicePattern.builder()
                .patternName("Date_MonthDayYear_YourFormat")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_DATE)
                .patternRegex("([A-Za-z]{3,9}\\s+\\d{1,2},?\\s+\\d{4})")
                .patternPriority(5)
                .confidenceWeight(BigDecimal.valueOf(1.0))
                .dateFormat("MMMM d, yyyy")
                .patternDescription("January 25, 2016")
                .notes("User's preferred date format")
                .build());

        // Alternative formats
        patterns.add(InvoicePattern.builder()
                .patternName("Date_MMDDYYYY")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_DATE)
                .patternRegex("(\\d{1,2}/\\d{1,2}/\\d{4})")
                .patternPriority(20)
                .confidenceWeight(BigDecimal.valueOf(0.8))
                .dateFormat("M/d/yyyy")
                .patternDescription("01/25/2016")
                .notes("US format MM/DD/YYYY")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("Date_DDMMYYYY")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_DATE)
                .patternRegex("(\\d{1,2}/\\d{1,2}/\\d{4})")
                .patternPriority(25)
                .confidenceWeight(BigDecimal.valueOf(0.7))
                .dateFormat("d/M/yyyy")
                .patternDescription("25/01/2016")
                .notes("EU format DD/MM/YYYY")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("Date_ISO")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_DATE)
                .patternRegex("(\\d{4}-\\d{2}-\\d{2})")
                .patternPriority(15)
                .confidenceWeight(BigDecimal.valueOf(0.9))
                .dateFormat("yyyy-MM-dd")
                .patternDescription("2016-01-25")
                .notes("ISO format YYYY-MM-DD")
                .build());

        return patterns;
    }

    /**
     * Vendor and customer patterns
     */
    private List<InvoicePattern> createVendorAndCustomerPatterns() {
        List<InvoicePattern> patterns = new ArrayList<>();

        // Traditional vendor patterns
        patterns.add(InvoicePattern.builder()
                .patternName("Vendor_Company")
                .patternCategory(InvoicePattern.PatternCategory.VENDOR)
                .patternRegex("(?i)(?:company|vendor|supplier):\\s*([A-Za-z0-9\\s\\-,.&]+)")
                .patternPriority(20)
                .confidenceWeight(BigDecimal.valueOf(0.8))
                .patternDescription("Company: ABC Corp")
                .notes("Traditional vendor format")
                .build());

        // Email patterns
        patterns.add(InvoicePattern.builder()
                .patternName("Email_Standard")
                .patternCategory(InvoicePattern.PatternCategory.EMAIL)
                .patternRegex("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})")
                .patternPriority(10)
                .confidenceWeight(BigDecimal.valueOf(0.9))
                .patternDescription("admin@example.com")
                .notes("Standard email pattern")
                .build());

        // Customer patterns
        patterns.add(InvoicePattern.builder()
                .patternName("Customer_To")
                .patternCategory(InvoicePattern.PatternCategory.CUSTOMER)
                .patternRegex("(?i)to:\\s*([A-Za-z0-9\\s\\-,.&]+?)(?=\\s*(?:\\d+|[A-Za-z]+\\s+[A-Za-z]+|$))")
                .patternPriority(10)
                .confidenceWeight(BigDecimal.valueOf(0.8))
                .patternDescription("To: Test Business")
                .notes("Customer billing information")
                .build());

        return patterns;
    }

    /**
     * Specialized patterns for edge cases
     */
    private List<InvoicePattern> createSpecializedPatterns() {
        List<InvoicePattern> patterns = new ArrayList<>();

        // Currency extraction
        patterns.add(InvoicePattern.builder()
                .patternName("Currency_Symbol")
                .patternCategory(InvoicePattern.PatternCategory.CURRENCY)
                .patternRegex("(?:\\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)")
                .patternPriority(10)
                .confidenceWeight(BigDecimal.valueOf(0.8))
                .patternDescription("$, USD, €, etc.")
                .notes("Currency symbol detection")
                .build());

        // Phone patterns
        patterns.add(InvoicePattern.builder()
                .patternName("Phone_Standard")
                .patternCategory(InvoicePattern.PatternCategory.PHONE)
                .patternRegex("(\\(?\\d{3}\\)?[-\\s]?\\d{3}[-\\s]?\\d{4})")
                .patternPriority(10)
                .confidenceWeight(BigDecimal.valueOf(0.7))
                .patternDescription("(555) 123-4567")
                .notes("US phone number format")
                .build());

        return patterns;
    }

    /**
     * Create patterns for previously failed invoice formats
     */
    private List<InvoicePattern> createFailedInvoicePatterns() {
        List<InvoicePattern> patterns = new ArrayList<>();

        // German Invoice Patterns (CPB Software format)
        patterns.add(InvoicePattern.builder()
                .patternName("German_InvoiceNo_CPB")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_NUMBER)
                .patternRegex("(?i)Invoice\\s+No\\.?\\s+([0-9]{6,})")
                .patternPriority(3)
                .confidenceWeight(BigDecimal.valueOf(0.95))
                .notes("German CPB Software invoice format")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("German_GrossAmount")
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?i)Gross\\s+Amount\\s+incl\\.?\\s+VAT\\s+([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2}))")
                .patternPriority(3)
                .confidenceWeight(BigDecimal.valueOf(0.90))
                .notes("German gross amount with VAT")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("German_Date_Marz")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_DATE)
                .patternRegex("(?i)Date\\s+([0-9]{1,2}\\.\\s*[A-Za-z]{3,9}\\s+[0-9]{4})")
                .patternPriority(3)
                .confidenceWeight(BigDecimal.valueOf(0.85))
                .dateFormat("dd. MMMM yyyy")
                .notes("German date format with month names")
                .build());

        // SuperStore Receipt Patterns
        patterns.add(InvoicePattern.builder()
                .patternName("Receipt_InvoiceHash")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_NUMBER)
                .patternRegex("(?i)INVOICE\\s*#\\s*([0-9]{4,})")
                .patternPriority(4)
                .confidenceWeight(BigDecimal.valueOf(0.92))
                .notes("Receipt style invoice with # symbol")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("Receipt_BalanceDue")
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?i)Balance\\s+Due:?\\s*\\$?([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternPriority(4)
                .confidenceWeight(BigDecimal.valueOf(0.95))
                .notes("Receipt balance due amount")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("Receipt_DateShort")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_DATE)
                .patternRegex("(?i)Date:?\\s+([A-Za-z]{3}\\s+[0-9]{1,2}\\s+[0-9]{4})")
                .patternPriority(4)
                .confidenceWeight(BigDecimal.valueOf(0.88))
                .dateFormat("MMM dd yyyy")
                .notes("Short date format: Nov 23 2012")
                .build());

        // French Galerie Patterns
        patterns.add(InvoicePattern.builder()
                .patternName("French_InvoiceNoDot")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_NUMBER)
                .patternRegex("(?i)Invoice\\s+No\\.\\s+([0-9]{3,})")
                .patternPriority(4)
                .confidenceWeight(BigDecimal.valueOf(0.93))
                .notes("French invoice with dot after No")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("French_TotalDue")
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?i)Total\\s+Due\\s+([0-9]{1,3}(?:[.,][0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternPriority(4)
                .confidenceWeight(BigDecimal.valueOf(0.94))
                .notes("French total due amount")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("European_DateDots")
                .patternCategory(InvoicePattern.PatternCategory.INVOICE_DATE)
                .patternRegex("([0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{4})")
                .patternPriority(4)
                .confidenceWeight(BigDecimal.valueOf(0.90))
                .dateFormat("dd.MM.yyyy")
                .notes("European date format with dots")
                .build());

        // Enhanced patterns for complex amounts
        patterns.add(InvoicePattern.builder()
                .patternName("Complex_AmountEUR")
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?i)(?:Total|Amount|Gross)\\s*[\\w\\s]*?\\s+([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2}))\\s*(?:EUR|€)")
                .patternPriority(5)
                .confidenceWeight(BigDecimal.valueOf(0.85))
                .notes("Complex amount patterns with EUR")
                .build());

        patterns.add(InvoicePattern.builder()
                .patternName("Complex_AmountUSD")
                .patternCategory(InvoicePattern.PatternCategory.AMOUNT)
                .patternRegex("(?i)(?:Total|Amount|Balance|Due)\\s*[\\w\\s]*?\\s*\\$([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)")
                .patternPriority(5)
                .confidenceWeight(BigDecimal.valueOf(0.85))
                .notes("Complex amount patterns with USD")
                .build());

        // Vendor patterns for complex formats
        patterns.add(InvoicePattern.builder()
                .patternName("Vendor_CompanyGmbH")
                .patternCategory(InvoicePattern.PatternCategory.VENDOR)
                .patternRegex("([A-Za-z][A-Za-z0-9\\s&.-]+(?:GmbH|AG|Ltd|LLC|Inc|Corp))")
                .patternPriority(5)
                .confidenceWeight(BigDecimal.valueOf(0.88))
                .notes("Company names with legal suffixes")
                .build());

        return patterns;
    }

    /**
     * Log summary of seeded patterns
     */
    private void logPatternSummary(List<InvoicePattern> patterns) {
        log.info("📊 Pattern Summary:");
        
        for (InvoicePattern.PatternCategory category : InvoicePattern.PatternCategory.values()) {
            long count = patterns.stream()
                    .filter(p -> p.getPatternCategory() == category)
                    .count();
            if (count > 0) {
                log.info("  {} {}: {} patterns", 
                        getCategoryEmoji(category), category.getDisplayName(), count);
            }
        }

        // Log high-priority patterns
        long highPriorityCount = patterns.stream()
                .filter(p -> p.getPatternPriority() <= 10)
                .count();
        log.info("⭐ High Priority Patterns (≤10): {}", highPriorityCount);
        
        // Log user-specific patterns
        long userSpecificCount = patterns.stream()
                .filter(p -> p.getNotes() != null && p.getNotes().toLowerCase().contains("user"))
                .count();
        log.info("🎯 User-Specific Patterns: {}", userSpecificCount);
    }

    private String getCategoryEmoji(InvoicePattern.PatternCategory category) {
        return switch (category) {
            case INVOICE_NUMBER -> "🔢";
            case AMOUNT -> "💰";
            case DATE, INVOICE_DATE, DUE_DATE -> "📅";
            case VENDOR -> "🏢";
            case CUSTOMER -> "👤";
            case EMAIL -> "📧";
            case PHONE -> "📞";
            case TAX_AMOUNT, SUBTOTAL_AMOUNT -> "💹";
            case CURRENCY -> "💱";
            case ADDRESS -> "📍";
            case PAYMENT_TERMS -> "📋";
        };
    }
}
