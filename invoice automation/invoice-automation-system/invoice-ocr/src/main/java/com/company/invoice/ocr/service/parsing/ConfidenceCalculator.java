package com.company.invoice.ocr.service.parsing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;


/**
 * Calculates confidence scores for extracted invoice data using multiple factors.
 */
@Component
@Slf4j
public class ConfidenceCalculator {

    // Field importance weights for overall confidence calculation
    private static final Map<String, Double> FIELD_WEIGHTS = Map.of(
        "invoice_number", 0.25,
        "total_amount", 0.25,
        "vendor_name", 0.20,
        "invoice_date", 0.15,
        "due_date", 0.10,
        "other_fields", 0.05
    );

    /**
     * Calculate overall confidence score for extracted invoice data
     */
    public double calculateOverallConfidence(InvoiceDataExtractor.ExtractedInvoiceData data,
                                           InvoiceDataExtractor.ValidationResult validation) {
        
        Map<String, Double> fieldConfidences = calculateFieldConfidences(data);
        
        // Calculate weighted average of field confidences
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        
        for (Map.Entry<String, Double> entry : fieldConfidences.entrySet()) {
            String field = entry.getKey();
            Double confidence = entry.getValue();
            Double weight = FIELD_WEIGHTS.getOrDefault(field, FIELD_WEIGHTS.get("other_fields"));
            
            if (confidence != null && weight != null) {
                weightedSum += confidence * weight;
                totalWeight += weight;
            }
        }
        
        double baseConfidence = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
        
        // Apply bonuses and penalties
        double consistencyBonus = calculateConsistencyBonus(data);
        double validationPenalty = calculateValidationPenalty(validation);
        double completenessBonus = calculateCompletenessBonus(data);
        
        double finalConfidence = baseConfidence + consistencyBonus - validationPenalty + completenessBonus;
        
        // Ensure confidence is between 0 and 1
        finalConfidence = Math.min(1.0, Math.max(0.0, finalConfidence));
        
        log.debug("Confidence calculation: base={:.3f}, consistency={:.3f}, validation={:.3f}, completeness={:.3f}, final={:.3f}",
                baseConfidence, consistencyBonus, validationPenalty, completenessBonus, finalConfidence);
        
        return finalConfidence;
    }

    /**
     * Calculate individual field confidences
     */
    private Map<String, Double> calculateFieldConfidences(InvoiceDataExtractor.ExtractedInvoiceData data) {
        Map<String, Double> confidences = new HashMap<>();
        
        // Get confidence from field extractions
        if (data.getFieldExtractions() != null) {
            for (InvoiceDataExtractor.FieldExtraction extraction : data.getFieldExtractions()) {
                String fieldName = extraction.getFieldName();
                double extractionConfidence = extraction.getConfidence();
                
                // Enhance confidence based on field-specific factors
                double enhancedConfidence = enhanceFieldConfidence(fieldName, extraction.getValue(), extractionConfidence);
                
                // Take the highest confidence if multiple extractions for same field
                confidences.merge(fieldName, enhancedConfidence, Math::max);
            }
        }
        
        // Calculate derived confidences for fields not directly extracted
        if (data.getInvoiceNumber() != null && !confidences.containsKey("invoice_number")) {
            confidences.put("invoice_number", calculateInvoiceNumberConfidence(data.getInvoiceNumber()));
        }
        
        if (data.getTotalAmount() != null && !confidences.containsKey("total_amount")) {
            confidences.put("total_amount", calculateAmountConfidence(data.getTotalAmount()));
        }
        
        if (data.getVendorName() != null && !confidences.containsKey("vendor_name")) {
            confidences.put("vendor_name", calculateVendorNameConfidence(data.getVendorName()));
        }
        
        return confidences;
    }

    /**
     * Enhance field confidence based on field-specific validation
     */
    private double enhanceFieldConfidence(String fieldName, String value, double baseConfidence) {
        switch (fieldName) {
            case "invoice_number":
                return enhanceInvoiceNumberConfidence(value, baseConfidence);
            case "total_amount":
            case "subtotal_amount":
            case "tax_amount":
                return enhanceAmountConfidence(value, baseConfidence);
            case "invoice_date":
            case "due_date":
                return enhanceDateConfidence(value, baseConfidence);
            case "vendor_name":
                return enhanceVendorNameConfidence(value, baseConfidence);
            case "vendor_email":
                return enhanceEmailConfidence(value, baseConfidence);
            default:
                return baseConfidence;
        }
    }

    /**
     * Enhance invoice number confidence
     */
    private double enhanceInvoiceNumberConfidence(String invoiceNumber, double baseConfidence) {
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            return 0.0;
        }
        
        double enhancement = 0.0;
        
        // Length check
        if (invoiceNumber.length() >= 4 && invoiceNumber.length() <= 20) {
            enhancement += 0.1;
        }
        
        // Format patterns
        if (invoiceNumber.matches("^[A-Z]{2,4}-[0-9]{4,8}$")) {
            enhancement += 0.2; // Standard format like "INV-2024001"
        } else if (invoiceNumber.matches("^[0-9]{6,10}$")) {
            enhancement += 0.15; // Pure numeric format
        } else if (invoiceNumber.matches("^[A-Z0-9]{6,12}$")) {
            enhancement += 0.1; // Mixed alphanumeric
        }
        
        return Math.min(1.0, baseConfidence + enhancement);
    }

    /**
     * Enhance amount confidence
     */
    private double enhanceAmountConfidence(String amountStr, double baseConfidence) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            double enhancement = 0.0;
            
            // Amount reasonableness
            if (amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(new BigDecimal("1000000")) < 0) {
                enhancement += 0.15;
            }
            
            // Decimal precision (common for currency)
            if (amount.scale() == 2) {
                enhancement += 0.1;
            }
            
            return Math.min(1.0, baseConfidence + enhancement);
            
        } catch (NumberFormatException e) {
            return Math.max(0.0, baseConfidence - 0.3); // Penalty for invalid format
        }
    }

    /**
     * Enhance date confidence
     */
    private double enhanceDateConfidence(String dateStr, double baseConfidence) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            LocalDate date = LocalDate.parse(dateStr);
            double enhancement = 0.0;
            
            // Date reasonableness (within last 2 years to next 1 year)
            LocalDate now = LocalDate.now();
            if (date.isAfter(now.minusYears(2)) && date.isBefore(now.plusYears(1))) {
                enhancement += 0.2;
            }
            
            return Math.min(1.0, baseConfidence + enhancement);
            
        } catch (Exception e) {
            return Math.max(0.0, baseConfidence - 0.2);
        }
    }

    /**
     * Enhance vendor name confidence
     */
    private double enhanceVendorNameConfidence(String vendorName, double baseConfidence) {
        if (vendorName == null || vendorName.trim().isEmpty()) {
            return 0.0;
        }
        
        double enhancement = 0.0;
        
        // Length check
        if (vendorName.length() >= 3 && vendorName.length() <= 100) {
            enhancement += 0.1;
        }
        
        // Not just numbers
        if (!vendorName.matches("^[0-9\\s,.-]+$")) {
            enhancement += 0.1;
        }
        
        // Contains letters
        if (vendorName.matches(".*[A-Za-z].*")) {
            enhancement += 0.1;
        }
        
        return Math.min(1.0, baseConfidence + enhancement);
    }

    /**
     * Enhance email confidence
     */
    private double enhanceEmailConfidence(String email, double baseConfidence) {
        if (email == null || email.trim().isEmpty()) {
            return 0.0;
        }
        
        // Simple email format validation
        if (email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return Math.min(1.0, baseConfidence + 0.2);
        } else {
            return Math.max(0.0, baseConfidence - 0.3);
        }
    }

    /**
     * Calculate confidence for invoice number based on format
     */
    private double calculateInvoiceNumberConfidence(String invoiceNumber) {
        return enhanceInvoiceNumberConfidence(invoiceNumber, 0.5);
    }

    /**
     * Calculate confidence for amount based on value
     */
    private double calculateAmountConfidence(BigDecimal amount) {
        if (amount == null) return 0.0;
        return enhanceAmountConfidence(amount.toString(), 0.5);
    }

    /**
     * Calculate confidence for vendor name
     */
    private double calculateVendorNameConfidence(String vendorName) {
        return enhanceVendorNameConfidence(vendorName, 0.5);
    }

    /**
     * Calculate consistency bonus based on cross-field validation
     */
    private double calculateConsistencyBonus(InvoiceDataExtractor.ExtractedInvoiceData data) {
        double bonus = 0.0;
        
        // Check if amounts are consistent
        if (data.getSubtotalAmount() != null && data.getTaxAmount() != null && data.getTotalAmount() != null) {
            BigDecimal calculatedTotal = data.getSubtotalAmount().add(data.getTaxAmount());
            BigDecimal difference = calculatedTotal.subtract(data.getTotalAmount()).abs();
            
            if (difference.compareTo(new BigDecimal("0.01")) <= 0) {
                bonus += 0.1; // Perfect match
            } else if (difference.compareTo(new BigDecimal("0.10")) <= 0) {
                bonus += 0.05; // Close match
            }
        }
        
        // Check date consistency
        if (data.getInvoiceDate() != null && data.getDueDate() != null) {
            if (data.getDueDate().isAfter(data.getInvoiceDate())) {
                bonus += 0.05; // Logical date order
            }
        }
        
        // Check if we have both required and optional fields
        int requiredFields = 0;
        int optionalFields = 0;
        
        if (data.getInvoiceNumber() != null) requiredFields++;
        if (data.getTotalAmount() != null) requiredFields++;
        if (data.getVendorName() != null) requiredFields++;
        
        if (data.getInvoiceDate() != null) optionalFields++;
        if (data.getDueDate() != null) optionalFields++;
        if (data.getVendorAddress() != null) optionalFields++;
        if (data.getVendorEmail() != null) optionalFields++;
        
        if (requiredFields == 3 && optionalFields >= 2) {
            bonus += 0.05; // Good field coverage
        }
        
        return Math.min(0.2, bonus); // Cap bonus at 0.2
    }

    /**
     * Calculate validation penalty based on errors and warnings
     */
    private double calculateValidationPenalty(InvoiceDataExtractor.ValidationResult validation) {
        if (validation == null) return 0.0;
        
        double penalty = 0.0;
        
        // Penalty for errors
        if (validation.getErrors() != null) {
            for (InvoiceDataExtractor.ValidationError error : validation.getErrors()) {
                switch (error.getSeverity()) {
                    case "ERROR":
                        penalty += 0.15;
                        break;
                    case "WARNING":
                        penalty += 0.05;
                        break;
                }
            }
        }
        
        // Penalty for warnings
        if (validation.getWarnings() != null) {
            penalty += validation.getWarnings().size() * 0.02;
        }
        
        return Math.min(0.3, penalty); // Cap penalty at 0.3
    }

    /**
     * Calculate completeness bonus
     */
    private double calculateCompletenessBonus(InvoiceDataExtractor.ExtractedInvoiceData data) {
        int totalPossibleFields = 9; // invoice_number, total_amount, vendor_name, etc.
        int extractedFields = data.getExtractedFieldCount();
        
        double completenessRatio = (double) extractedFields / totalPossibleFields;
        
        // Bonus for high completeness
        if (completenessRatio >= 0.8) {
            return 0.1;
        } else if (completenessRatio >= 0.6) {
            return 0.05;
        }
        
        return 0.0;
    }
}
