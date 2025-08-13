package com.company.invoice.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for dynamic invoice pattern management.
 * Stores regex patterns for extracting different fields from invoice text.
 */
@Entity
@Table(name = "invoice_patterns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoicePattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pattern_name", nullable = false, length = 100)
    private String patternName;

    @Enumerated(EnumType.STRING)
    @Column(name = "pattern_category", nullable = false, length = 50)
    private PatternCategory patternCategory;

    @Lob
    @Column(name = "pattern_regex", nullable = false, columnDefinition = "TEXT")
    private String patternRegex;

    @Column(name = "pattern_flags")
    @Builder.Default
    private Integer patternFlags = 2; // Pattern.CASE_INSENSITIVE

    @Lob
    @Column(name = "pattern_description", columnDefinition = "TEXT")
    private String patternDescription;

    @Column(name = "pattern_priority")
    @Builder.Default
    private Integer patternPriority = 100; // Lower = higher priority

    @Column(name = "confidence_weight", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal confidenceWeight = BigDecimal.ONE;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "date_format", length = 50)
    private String dateFormat; // For date patterns: 'MM/dd/yyyy', etc.

    @Column(name = "capture_group")
    @Builder.Default
    private Integer captureGroup = 1; // Which regex group contains the value

    @Lob
    @Column(name = "validation_regex", columnDefinition = "TEXT")
    private String validationRegex; // Optional additional validation

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    @Builder.Default
    private String createdBy = "SYSTEM";

    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Pattern categories for invoice data extraction
     */
    public enum PatternCategory {
        INVOICE_NUMBER("Invoice Number"),
        AMOUNT("Total Amount"),
        DATE("Date Fields"),
        VENDOR("Vendor/Company"),
        ADDRESS("Address"),
        TAX_AMOUNT("Tax Amount"),
        SUBTOTAL_AMOUNT("Subtotal Amount"),
        DUE_DATE("Due Date"),
        INVOICE_DATE("Invoice Date"),
        CUSTOMER("Customer Information"),
        CURRENCY("Currency"),
        EMAIL("Email Address"),
        PHONE("Phone Number"),
        PAYMENT_TERMS("Payment Terms");

        private final String displayName;

        PatternCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Helper method to check if pattern is valid and active
     */
    public boolean isValidAndActive() {
        return isActive != null && isActive && 
               patternRegex != null && !patternRegex.trim().isEmpty();
    }

    /**
     * Helper method to get effective confidence weight (clamped between 0.1 and 1.0)
     */
    public BigDecimal getEffectiveConfidenceWeight() {
        if (confidenceWeight == null) {
            return BigDecimal.ONE;
        }
        if (confidenceWeight.compareTo(BigDecimal.valueOf(0.1)) < 0) {
            return BigDecimal.valueOf(0.1);
        }
        if (confidenceWeight.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return confidenceWeight;
    }

    /**
     * Helper method to get effective priority (defaults to 100 if null)
     */
    public Integer getEffectivePriority() {
        return patternPriority != null ? patternPriority : 100;
    }

    /**
     * Helper method to get effective capture group (defaults to 1 if null)
     */
    public Integer getEffectiveCaptureGroup() {
        return captureGroup != null ? captureGroup : 1;
    }

    @Override
    public String toString() {
        return String.format("InvoicePattern{id=%d, name='%s', category=%s, priority=%d, active=%s}",
                id, patternName, patternCategory, getEffectivePriority(), isActive);
    }
}
