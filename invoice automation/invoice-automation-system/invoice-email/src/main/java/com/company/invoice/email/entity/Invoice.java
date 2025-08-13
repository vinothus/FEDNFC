package com.company.invoice.email.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Invoice entity for storing PDF invoices with BLOB data and download links.
 * Supports both H2 (development) and PostgreSQL (production) databases.
 */
@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // File Information
    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    @Builder.Default
    private String contentType = "application/pdf";

    @Column(name = "checksum", length = 64)
    private String checksum;

    // PDF BLOB Storage
    @Lob
    @Column(name = "pdf_blob", columnDefinition = "BYTEA")
    private byte[] pdfBlob;

    // Download Link Management
    @Column(name = "download_token", unique = true, nullable = false, length = 128)
    private String downloadToken;

    @Column(name = "download_expires_at")
    private LocalDateTime downloadExpiresAt;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(name = "max_downloads")
    @Builder.Default
    private Integer maxDownloads = 10;

    @Column(name = "download_url", length = 500)
    private String downloadUrl; // Complete download URL for the PDF

    // Email Source Information
    @Column(name = "email_subject", length = 500)
    private String emailSubject;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    // Processing Status
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "ocr_status")
    @Builder.Default
    private OcrStatus ocrStatus = OcrStatus.PENDING;

    @Column(name = "ocr_confidence", precision = 5, scale = 2)
    private BigDecimal ocrConfidence;

    // Raw extracted text and OCR metadata
    @Lob
    @Column(name = "raw_extracted_text", columnDefinition = "TEXT")
    private String rawExtractedText;

    @Column(name = "ocr_method", length = 50)
    private String ocrMethod;

    @Column(name = "ocr_processing_time_ms")
    private Integer ocrProcessingTimeMs;

    @Column(name = "text_word_count")
    private Integer textWordCount;

    @Column(name = "text_character_count")
    private Integer textCharacterCount;

    // Pattern tracking for extraction analytics
    @Column(name = "used_pattern_ids", columnDefinition = "TEXT")
    private String usedPatternIds; // Comma-separated pattern IDs that matched

    @Column(name = "pattern_match_summary", columnDefinition = "TEXT")
    private String patternMatchSummary; // Human-readable summary of pattern matches

    @Lob
    @Column(name = "extraction_confidence_details", columnDefinition = "TEXT")
    private String extractionConfidenceDetails; // JSON string with detailed confidence per field

    // Invoice Data (extracted via OCR)
    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    // Audit Fields
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    // Enums for status tracking
    public enum ProcessingStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public enum OcrStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED
    }

    /**
     * Check if download link is still valid
     */
    public boolean isDownloadValid() {
        return downloadExpiresAt != null && 
               downloadExpiresAt.isAfter(LocalDateTime.now()) &&
               downloadCount < maxDownloads;
    }

    /**
     * Increment download count
     */
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }

    /**
     * Check if PDF BLOB is available
     */
    public boolean hasPdfBlob() {
        return pdfBlob != null && pdfBlob.length > 0;
    }

    /**
     * Get PDF BLOB size in bytes
     */
    public long getPdfBlobSize() {
        return pdfBlob != null ? pdfBlob.length : 0;
    }
}
