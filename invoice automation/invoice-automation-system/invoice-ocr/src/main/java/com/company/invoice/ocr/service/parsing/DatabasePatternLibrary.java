package com.company.invoice.ocr.service.parsing;

import com.company.invoice.data.entity.InvoicePattern;
import com.company.invoice.data.repository.InvoicePatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database-driven pattern library for dynamic invoice data extraction.
 * Replaces the hardcoded InvoicePatternLibrary with database-stored patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabasePatternLibrary {

    private final InvoicePatternRepository patternRepository;
    
    // Cache compiled patterns for performance
    private final Map<Long, Pattern> compiledPatternCache = new ConcurrentHashMap<>();
    private final Map<String, DateTimeFormatter> dateFormatterCache = new ConcurrentHashMap<>();

    /**
     * Extract invoice number using database patterns
     */
    public ExtractionResult extractInvoiceNumber(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.INVOICE_NUMBER);
    }

    /**
     * Extract total amount using database patterns
     */
    public ExtractionResult extractTotalAmount(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.AMOUNT);
    }

    /**
     * Extract invoice date using database patterns
     */
    public ExtractionResult extractInvoiceDate(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.INVOICE_DATE, InvoicePattern.PatternCategory.DATE);
    }

    /**
     * Extract due date using database patterns
     */
    public ExtractionResult extractDueDate(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.DUE_DATE, InvoicePattern.PatternCategory.DATE);
    }

    /**
     * Extract vendor name using database patterns
     */
    public ExtractionResult extractVendorName(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.VENDOR);
    }

    /**
     * Extract subtotal amount using database patterns
     */
    public ExtractionResult extractSubtotalAmount(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.SUBTOTAL_AMOUNT);
    }

    /**
     * Extract tax amount using database patterns
     */
    public ExtractionResult extractTaxAmount(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.TAX_AMOUNT);
    }

    /**
     * Extract currency using database patterns
     */
    public ExtractionResult extractCurrency(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.CURRENCY);
    }

    /**
     * Extract email address using database patterns
     */
    public ExtractionResult extractEmail(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.EMAIL);
    }

    /**
     * Extract customer information using database patterns
     */
    public ExtractionResult extractCustomer(String text) {
        return extractFieldByCategory(text, InvoicePattern.PatternCategory.CUSTOMER);
    }

    /**
     * Generic extraction method for multiple categories (fallback order)
     */
    private ExtractionResult extractFieldByCategory(String text, InvoicePattern.PatternCategory... categories) {
        if (text == null || text.trim().isEmpty()) {
            return new ExtractionResult(null, 0.0, "No text provided");
        }

        log.debug("🔍 Extracting field from categories: {} | Text length: {}", 
                 Arrays.toString(categories), text.length());

        for (InvoicePattern.PatternCategory category : categories) {
            List<InvoicePattern> patterns = getActivePatternssByCategory(category);
            
            log.debug("📋 Found {} active patterns for category: {}", patterns.size(), category);

            for (InvoicePattern dbPattern : patterns) {
                try {
                    Pattern compiledPattern = getCompiledPattern(dbPattern);
                    Matcher matcher = compiledPattern.matcher(text);

                    if (matcher.find()) {
                        int captureGroup = dbPattern.getEffectiveCaptureGroup();
                        
                        if (captureGroup <= matcher.groupCount()) {
                            String extractedValue = matcher.group(captureGroup);
                            
                            if (extractedValue != null && !extractedValue.trim().isEmpty()) {
                                // Apply additional validation if specified
                                if (!isValidExtraction(extractedValue, dbPattern)) {
                                    log.debug("❌ Extraction failed validation: {} | Pattern: {}", 
                                             extractedValue, dbPattern.getPatternName());
                                    continue;
                                }

                                // Process date fields
                                if (category == InvoicePattern.PatternCategory.DATE || 
                                    category == InvoicePattern.PatternCategory.INVOICE_DATE ||
                                    category == InvoicePattern.PatternCategory.DUE_DATE) {
                                    
                                    LocalDate parsedDate = parseDate(extractedValue, dbPattern.getDateFormat());
                                    if (parsedDate != null) {
                                        extractedValue = parsedDate.toString(); // ISO format
                                    } else {
                                        log.debug("❌ Failed to parse date: {} | Format: {}", 
                                                 extractedValue, dbPattern.getDateFormat());
                                        continue;
                                    }
                                }

                                // Calculate confidence based on pattern weight and match quality
                                double confidence = calculateConfidence(dbPattern, extractedValue, matcher);

                                log.info("✅ Field extracted: {} | Value: '{}' | Confidence: {}% | Pattern: '{}'",
                                        category, extractedValue, String.format("%.1f", confidence * 100), 
                                        dbPattern.getPatternName());

                                return new ExtractionResult(extractedValue, confidence, dbPattern.getPatternName());
                            }
                        } else {
                            log.warn("⚠️ Capture group {} exceeds available groups in pattern: {}", 
                                    captureGroup, dbPattern.getPatternName());
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Error applying pattern '{}': {}", dbPattern.getPatternName(), e.getMessage());
                }
            }
        }

        log.debug("❌ No patterns matched for categories: {}", Arrays.toString(categories));
        return new ExtractionResult(null, 0.0, "No matching patterns found");
    }

    /**
     * Get compiled regex pattern with caching
     */
    private Pattern getCompiledPattern(InvoicePattern dbPattern) {
        return compiledPatternCache.computeIfAbsent(dbPattern.getId(), id -> {
            try {
                int flags = dbPattern.getPatternFlags() != null ? dbPattern.getPatternFlags() : Pattern.CASE_INSENSITIVE;
                return Pattern.compile(dbPattern.getPatternRegex(), flags);
            } catch (Exception e) {
                log.error("❌ Failed to compile pattern '{}': {}", dbPattern.getPatternName(), e.getMessage());
                // Return a pattern that never matches
                return Pattern.compile("(?!)");
            }
        });
    }

    /**
     * Get active patterns by category with caching
     */
    @Cacheable(value = "patternsByCategory", key = "#category")
    private List<InvoicePattern> getActivePatternssByCategory(InvoicePattern.PatternCategory category) {
        return patternRepository.findActivePatternssByCategory(category);
    }

    /**
     * Validate extracted value using optional validation regex
     */
    private boolean isValidExtraction(String value, InvoicePattern dbPattern) {
        if (dbPattern.getValidationRegex() == null || dbPattern.getValidationRegex().trim().isEmpty()) {
            return true; // No validation required
        }

        try {
            Pattern validationPattern = Pattern.compile(dbPattern.getValidationRegex());
            return validationPattern.matcher(value).matches();
        } catch (Exception e) {
            log.warn("⚠️ Invalid validation regex in pattern '{}': {}", 
                    dbPattern.getPatternName(), e.getMessage());
            return true; // Allow if validation pattern is invalid
        }
    }

    /**
     * Parse date string using specified format
     */
    private LocalDate parseDate(String dateStr, String dateFormat) {
        if (dateFormat == null || dateFormat.trim().isEmpty()) {
            // Try common formats
            return tryCommonDateFormats(dateStr);
        }

        try {
            DateTimeFormatter formatter = dateFormatterCache.computeIfAbsent(dateFormat, 
                    format -> DateTimeFormatter.ofPattern(format, Locale.ENGLISH));
            return LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            log.debug("❌ Failed to parse date '{}' with format '{}': {}", 
                     dateStr, dateFormat, e.getMessage());
            return tryCommonDateFormats(dateStr);
        }
    }

    /**
     * Try parsing with common date formats
     */
    private LocalDate tryCommonDateFormats(String dateStr) {
        String[] commonFormats = {
            "MMMM d, yyyy", "MMM d, yyyy", "MM/dd/yyyy", "M/d/yyyy",
            "dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "dd-MM-yyyy"
        };

        for (String format : commonFormats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH);
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }
        return null;
    }

    /**
     * Calculate confidence score for an extraction
     */
    private double calculateConfidence(InvoicePattern dbPattern, String extractedValue, Matcher matcher) {
        double baseConfidence = dbPattern.getEffectiveConfidenceWeight().doubleValue();

        // Adjust confidence based on match quality
        double qualityMultiplier = 1.0;

        // Full word boundary matches get higher confidence
        if (matcher.start() == 0 || Character.isWhitespace(matcher.group().charAt(0))) {
            qualityMultiplier += 0.1;
        }

        // Longer matches (within reason) get slightly higher confidence
        if (extractedValue.length() > 3 && extractedValue.length() < 50) {
            qualityMultiplier += 0.05;
        }

        // Pattern priority affects confidence (lower priority number = higher confidence)
        int priority = dbPattern.getEffectivePriority();
        if (priority <= 10) {
            qualityMultiplier += 0.1; // High priority patterns
        } else if (priority <= 50) {
            qualityMultiplier += 0.05; // Medium priority patterns
        }

        return Math.min(1.0, baseConfidence * qualityMultiplier);
    }

    /**
     * Clear pattern cache (useful when patterns are updated)
     */
    public void clearPatternCache() {
        compiledPatternCache.clear();
        log.info("🔄 Pattern cache cleared");
    }

    /**
     * Get pattern statistics for monitoring
     */
    public Map<String, Object> getPatternStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalPatterns = patternRepository.count();
        List<InvoicePattern> activePatterns = patternRepository.findAllActivePatternsOrderedByPriority();
        
        stats.put("totalPatterns", totalPatterns);
        stats.put("activePatterns", activePatterns.size());
        stats.put("cachedPatterns", compiledPatternCache.size());
        
        // Count by category
        Map<String, Long> categoryCount = new HashMap<>();
        for (InvoicePattern.PatternCategory category : InvoicePattern.PatternCategory.values()) {
            long count = patternRepository.countActivePatternssByCategory(category);
            categoryCount.put(category.name(), count);
        }
        stats.put("categoryCounts", categoryCount);
        
        return stats;
    }

    /**
     * Test all patterns against sample text (for debugging)
     */
    public List<TestResult> testAllPatterns(String sampleText) {
        List<TestResult> results = new ArrayList<>();
        
        List<InvoicePattern> allActivePatterns = patternRepository.findAllActivePatternsOrderedByPriority();
        
        for (InvoicePattern dbPattern : allActivePatterns) {
            try {
                Pattern compiledPattern = getCompiledPattern(dbPattern);
                Matcher matcher = compiledPattern.matcher(sampleText);
                
                if (matcher.find()) {
                    int captureGroup = dbPattern.getEffectiveCaptureGroup();
                    String extractedValue = captureGroup <= matcher.groupCount() ? 
                            matcher.group(captureGroup) : "No capture group " + captureGroup;
                    
                    double confidence = calculateConfidence(dbPattern, extractedValue, matcher);
                    
                    results.add(new TestResult(
                            dbPattern.getPatternName(),
                            dbPattern.getPatternCategory().name(),
                            extractedValue,
                            confidence,
                            true,
                            null
                    ));
                } else {
                    results.add(new TestResult(
                            dbPattern.getPatternName(),
                            dbPattern.getPatternCategory().name(),
                            null,
                            0.0,
                            false,
                            "No match"
                    ));
                }
            } catch (Exception e) {
                results.add(new TestResult(
                        dbPattern.getPatternName(),
                        dbPattern.getPatternCategory().name(),
                        null,
                        0.0,
                        false,
                        "Error: " + e.getMessage()
                ));
            }
        }
        
        return results;
    }

    /**
     * Result of field extraction
     */
    public record ExtractionResult(String value, double confidence, String patternUsed) {}

    /**
     * Result of pattern testing
     */
    public record TestResult(String patternName, String category, String extractedValue, 
                           double confidence, boolean matched, String error) {}
}
