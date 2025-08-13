package com.company.invoice.ocr.service.parsing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates extracted invoice data for accuracy and business logic compliance.
 */
@Component
@Slf4j
public class ExtractionValidator {

    /**
     * Validate extracted invoice data
     */
    public InvoiceDataExtractor.ValidationResult validate(InvoiceDataExtractor.ExtractedInvoiceData data) {
        List<InvoiceDataExtractor.ValidationError> errors = new ArrayList<>();
        List<InvoiceDataExtractor.ValidationWarning> warnings = new ArrayList<>();

        // Required field validation
        validateRequiredFields(data, errors);

        // Format validation
        validateFormats(data, errors, warnings);

        // Business logic validation
        validateBusinessLogic(data, errors, warnings);

        // Cross-field validation
        validateCrossFields(data, errors, warnings);

        return InvoiceDataExtractor.ValidationResult.builder()
                .isValid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .validatedAt(Instant.now())
                .build();
    }

    /**
     * Validate required fields
     */
    private void validateRequiredFields(InvoiceDataExtractor.ExtractedInvoiceData data, 
                                       List<InvoiceDataExtractor.ValidationError> errors) {
        
        if (data.getInvoiceNumber() == null || data.getInvoiceNumber().trim().isEmpty()) {
            errors.add(InvoiceDataExtractor.ValidationError.builder()
                    .field("invoice_number")
                    .errorType("MISSING_REQUIRED")
                    .message("Invoice number is required")
                    .severity("ERROR")
                    .build());
        }

        if (data.getTotalAmount() == null) {
            errors.add(InvoiceDataExtractor.ValidationError.builder()
                    .field("total_amount")
                    .errorType("MISSING_REQUIRED")
                    .message("Total amount is required")
                    .severity("ERROR")
                    .build());
        }

        if (data.getVendorName() == null || data.getVendorName().trim().isEmpty()) {
            errors.add(InvoiceDataExtractor.ValidationError.builder()
                    .field("vendor_name")
                    .errorType("MISSING_REQUIRED")
                    .message("Vendor name is required")
                    .severity("ERROR")
                    .build());
        }
    }

    /**
     * Validate field formats
     */
    private void validateFormats(InvoiceDataExtractor.ExtractedInvoiceData data,
                                List<InvoiceDataExtractor.ValidationError> errors,
                                List<InvoiceDataExtractor.ValidationWarning> warnings) {

        // Invoice number format
        if (data.getInvoiceNumber() != null && 
            !data.getInvoiceNumber().matches("^[A-Za-z0-9\\-]{3,20}$")) {
            warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                    .field("invoice_number")
                    .warningType("UNUSUAL_FORMAT")
                    .message("Invoice number format appears unusual")
                    .suggestedValue(normalizeInvoiceNumber(data.getInvoiceNumber()))
                    .build());
        }

        // Amount validation
        if (data.getTotalAmount() != null && 
            data.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(InvoiceDataExtractor.ValidationError.builder()
                    .field("total_amount")
                    .errorType("INVALID_VALUE")
                    .message("Total amount must be positive")
                    .severity("ERROR")
                    .build());
        }

        // Large amount warning
        if (data.getTotalAmount() != null && 
            data.getTotalAmount().compareTo(new BigDecimal("50000")) > 0) {
            warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                    .field("total_amount")
                    .warningType("LARGE_AMOUNT")
                    .message("Large invoice amount - please verify")
                    .build());
        }

        // Date validation
        validateDateFormats(data, errors, warnings);

        // Email format validation
        if (data.getVendorEmail() != null && 
            !data.getVendorEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                    .field("vendor_email")
                    .warningType("INVALID_FORMAT")
                    .message("Vendor email format appears invalid")
                    .build());
        }
    }

    /**
     * Validate date formats and reasonableness
     */
    private void validateDateFormats(InvoiceDataExtractor.ExtractedInvoiceData data,
                                    List<InvoiceDataExtractor.ValidationError> errors,
                                    List<InvoiceDataExtractor.ValidationWarning> warnings) {
        
        LocalDate now = LocalDate.now();

        // Invoice date validation
        if (data.getInvoiceDate() != null) {
            if (data.getInvoiceDate().isAfter(now.plusDays(1))) {
                warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                        .field("invoice_date")
                        .warningType("FUTURE_DATE")
                        .message("Invoice date is in the future")
                        .build());
            }

            if (data.getInvoiceDate().isBefore(now.minusYears(2))) {
                warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                        .field("invoice_date")
                        .warningType("OLD_DATE")
                        .message("Invoice date is more than 2 years old")
                        .build());
            }
        }

        // Due date validation
        if (data.getDueDate() != null) {
            if (data.getDueDate().isBefore(now.minusMonths(6))) {
                warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                        .field("due_date")
                        .warningType("OVERDUE")
                        .message("Invoice appears to be significantly overdue")
                        .build());
            }

            if (data.getDueDate().isAfter(now.plusYears(1))) {
                warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                        .field("due_date")
                        .warningType("DISTANT_FUTURE")
                        .message("Due date is unusually far in the future")
                        .build());
            }
        }
    }

    /**
     * Validate business logic rules
     */
    private void validateBusinessLogic(InvoiceDataExtractor.ExtractedInvoiceData data,
                                      List<InvoiceDataExtractor.ValidationError> errors,
                                      List<InvoiceDataExtractor.ValidationWarning> warnings) {

        // Date logic validation
        if (data.getInvoiceDate() != null && data.getDueDate() != null) {
            if (data.getDueDate().isBefore(data.getInvoiceDate())) {
                errors.add(InvoiceDataExtractor.ValidationError.builder()
                        .field("due_date")
                        .errorType("INVALID_LOGIC")
                        .message("Due date cannot be before invoice date")
                        .severity("ERROR")
                        .build());
            }

            // Check if payment terms are reasonable (within 90 days)
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(data.getInvoiceDate(), data.getDueDate());
            if (daysBetween > 90) {
                warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                        .field("due_date")
                        .warningType("LONG_PAYMENT_TERMS")
                        .message("Payment terms exceed 90 days")
                        .build());
            }
        }

        // Amount relationships validation
        if (data.getSubtotalAmount() != null && data.getTotalAmount() != null) {
            if (data.getSubtotalAmount().compareTo(data.getTotalAmount()) > 0) {
                errors.add(InvoiceDataExtractor.ValidationError.builder()
                        .field("subtotal_amount")
                        .errorType("INVALID_LOGIC")
                        .message("Subtotal cannot be greater than total amount")
                        .severity("ERROR")
                        .build());
            }
        }

        // Tax amount validation
        if (data.getTaxAmount() != null && data.getSubtotalAmount() != null) {
            // Check if tax rate is reasonable (0-50%)
            BigDecimal taxRate = data.getTaxAmount().divide(data.getSubtotalAmount(), 4, java.math.RoundingMode.HALF_UP);
            if (taxRate.compareTo(new BigDecimal("0.5")) > 0) {
                warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                        .field("tax_amount")
                        .warningType("HIGH_TAX_RATE")
                        .message("Tax rate appears unusually high (>" + taxRate.multiply(new BigDecimal("100")).intValue() + "%)")
                        .build());
            }
        }
    }

    /**
     * Validate cross-field consistency
     */
    private void validateCrossFields(InvoiceDataExtractor.ExtractedInvoiceData data,
                                    List<InvoiceDataExtractor.ValidationError> errors,
                                    List<InvoiceDataExtractor.ValidationWarning> warnings) {

        // Check amount consistency
        if (data.getSubtotalAmount() != null && data.getTaxAmount() != null && data.getTotalAmount() != null) {
            BigDecimal calculatedTotal = data.getSubtotalAmount().add(data.getTaxAmount());
            BigDecimal difference = calculatedTotal.subtract(data.getTotalAmount()).abs();

            // Allow for small rounding differences (up to $0.10)
            if (difference.compareTo(new BigDecimal("0.10")) > 0) {
                errors.add(InvoiceDataExtractor.ValidationError.builder()
                        .field("total_amount")
                        .errorType("AMOUNT_MISMATCH")
                        .message("Total amount does not match subtotal + tax (difference: $" + difference + ")")
                        .severity("ERROR")
                        .build());
            } else if (difference.compareTo(new BigDecimal("0.01")) > 0) {
                warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                        .field("total_amount")
                        .warningType("MINOR_AMOUNT_MISMATCH")
                        .message("Minor difference in amount calculation (difference: $" + difference + ")")
                        .build());
            }
        }

        // Vendor consistency checks
        if (data.getVendorName() != null && data.getVendorEmail() != null) {
            String emailDomain = extractDomainFromEmail(data.getVendorEmail());
            if (emailDomain != null && !isVendorNameEmailConsistent(data.getVendorName(), emailDomain)) {
                warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                        .field("vendor_name")
                        .warningType("VENDOR_EMAIL_MISMATCH")
                        .message("Vendor name and email domain may not match")
                        .build());
            }
        }

        // Currency consistency
        if (data.getCurrency() != null && !isValidCurrency(data.getCurrency())) {
            warnings.add(InvoiceDataExtractor.ValidationWarning.builder()
                    .field("currency")
                    .warningType("UNUSUAL_CURRENCY")
                    .message("Currency code is not commonly used")
                    .build());
        }
    }

    /**
     * Normalize invoice number format
     */
    private String normalizeInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null) return null;
        
        // Remove special characters except hyphens and alphanumeric
        return invoiceNumber.replaceAll("[^A-Za-z0-9\\-]", "").toUpperCase();
    }

    /**
     * Extract domain from email address
     */
    private String extractDomainFromEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        
        String domain = email.substring(email.indexOf('@') + 1);
        return domain.contains(".") ? domain.substring(0, domain.indexOf('.')) : domain;
    }

    /**
     * Check if vendor name and email domain are consistent
     */
    private boolean isVendorNameEmailConsistent(String vendorName, String emailDomain) {
        if (vendorName == null || emailDomain == null) return true;
        
        String normalizedVendor = vendorName.toLowerCase().replaceAll("[^a-z0-9]", "");
        String normalizedDomain = emailDomain.toLowerCase().replaceAll("[^a-z0-9]", "");
        
        // Check if domain contains vendor name or vice versa
        return normalizedVendor.contains(normalizedDomain) || 
               normalizedDomain.contains(normalizedVendor) ||
               calculateStringSimilarity(normalizedVendor, normalizedDomain) > 0.7;
    }

    /**
     * Check if currency code is valid/common
     */
    private boolean isValidCurrency(String currency) {
        if (currency == null) return false;
        
        List<String> commonCurrencies = List.of("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CHF", "CNY", "INR");
        return commonCurrencies.contains(currency.toUpperCase());
    }

    /**
     * Calculate string similarity using a simple algorithm
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int editDistance = calculateEditDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLen;
    }

    /**
     * Calculate edit distance between two strings
     */
    private int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }
        
        return dp[m][n];
    }
}
