package com.company.invoice.api.dto.response;

import com.company.invoice.email.entity.Invoice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Full invoice response for invoice management operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete invoice information")
public class InvoiceResponse {

    @Schema(description = "Invoice ID", example = "1")
    private String id;

    @Schema(description = "Original filename", example = "ACME_Invoice_2025_001.pdf")
    private String fileName;

    @Schema(description = "Upload/Creation date")
    private String uploadDate;

    @Schema(description = "Processing status", example = "COMPLETED")
    private String status;

    @Schema(description = "Total amount", example = "1250.00")
    private BigDecimal totalAmount;

    @Schema(description = "Currency", example = "USD")
    private String currency;

    @Schema(description = "Vendor name", example = "ACME Corporation")
    private String vendorName;

    @Schema(description = "Invoice number", example = "INV-2025-001")
    private String invoiceNumber;

    @Schema(description = "Invoice date")
    private String invoiceDate;

    @Schema(description = "Due date")
    private String dueDate;

    @Schema(description = "Error message (if processing failed)")
    private String errorMessage;

    @Schema(description = "Email subject", example = "Invoice from ACME Corp")
    private String emailSubject;

    @Schema(description = "Sender email", example = "billing@acmecorp.com")
    private String senderEmail;

    @Schema(description = "Received date")
    private String receivedDate;

    @Schema(description = "OCR status", example = "COMPLETED")
    private String ocrStatus;

    @Schema(description = "OCR confidence", example = "95.5")
    private BigDecimal ocrConfidence;

    @Schema(description = "File size in bytes", example = "245760")
    private Long fileSize;

    @Schema(description = "Download URL", example = "http://localhost:8080/invoice-automation/pdf/download/abc123")
    private String downloadUrl;

    /**
     * Convert Invoice entity to InvoiceResponse DTO
     */
    public static InvoiceResponse fromInvoice(Invoice invoice) {
        return InvoiceResponse.builder()
            .id(invoice.getId() != null ? invoice.getId().toString() : null)
            .fileName(invoice.getOriginalFilename())
            .uploadDate(invoice.getCreatedAt() != null ? invoice.getCreatedAt().toString() : null)
            .status(invoice.getProcessingStatus() != null ? invoice.getProcessingStatus().name() : "PENDING")
            .totalAmount(invoice.getTotalAmount())
            .currency(invoice.getCurrency())
            .vendorName(invoice.getVendorName())
            .invoiceNumber(invoice.getInvoiceNumber())
            .invoiceDate(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : null)
            .dueDate(invoice.getDueDate() != null ? invoice.getDueDate().toString() : null)
            .errorMessage(null) // We can add error tracking later if needed
            .emailSubject(invoice.getEmailSubject())
            .senderEmail(invoice.getSenderEmail())
            .receivedDate(invoice.getReceivedDate() != null ? invoice.getReceivedDate().toString() : null)
            .ocrStatus(invoice.getOcrStatus() != null ? invoice.getOcrStatus().name() : "PENDING")
            .ocrConfidence(invoice.getOcrConfidence())
            .fileSize(invoice.getFileSize())
            .downloadUrl(invoice.getDownloadUrl())
            .build();
    }
}
