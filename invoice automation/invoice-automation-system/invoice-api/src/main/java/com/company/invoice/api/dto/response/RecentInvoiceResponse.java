package com.company.invoice.api.dto.response;

import com.company.invoice.email.entity.Invoice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Recent invoice response for dashboard display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recent invoice information for dashboard")
public class RecentInvoiceResponse {

    @Schema(description = "Invoice ID", example = "1")
    private Long id;

    @Schema(description = "Invoice number", example = "INV-2025-001")
    private String invoiceNumber;

    @Schema(description = "Vendor name", example = "ACME Corporation")
    private String vendorName;

    @Schema(description = "Total amount", example = "1250.00")
    private BigDecimal totalAmount;

    @Schema(description = "Currency", example = "USD")
    private String currency;

    @Schema(description = "Invoice date")
    private LocalDateTime invoiceDate;

    @Schema(description = "Received date")
    private LocalDateTime receivedDate;

    @Schema(description = "Processing status", example = "COMPLETED")
    private String processingStatus;

    @Schema(description = "OCR status", example = "COMPLETED")
    private String ocrStatus;

    @Schema(description = "OCR confidence", example = "95.5")
    private BigDecimal ocrConfidence;

    @Schema(description = "Email subject", example = "Invoice from ACME Corp")
    private String emailSubject;

    @Schema(description = "Sender email", example = "billing@acmecorp.com")
    private String senderEmail;

    @Schema(description = "Original filename", example = "ACME_Invoice_2025_001.pdf")
    private String originalFilename;

    @Schema(description = "File size in bytes", example = "245760")
    private Long fileSize;

    @Schema(description = "Download URL", example = "http://localhost:8080/invoice-automation/pdf/download/abc123")
    private String downloadUrl;

    /**
     * Convert Invoice entity to RecentInvoiceResponse DTO
     */
    public static RecentInvoiceResponse fromInvoice(Invoice invoice) {
        return RecentInvoiceResponse.builder()
            .id(invoice.getId())
            .invoiceNumber(invoice.getInvoiceNumber())
            .vendorName(invoice.getVendorName())
            .totalAmount(invoice.getTotalAmount())
            .currency(invoice.getCurrency())
            .invoiceDate(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().atStartOfDay() : null)
            .receivedDate(invoice.getReceivedDate())
            .processingStatus(invoice.getProcessingStatus() != null ? invoice.getProcessingStatus().name() : null)
            .ocrStatus(invoice.getOcrStatus() != null ? invoice.getOcrStatus().name() : null)
            .ocrConfidence(invoice.getOcrConfidence())
            .emailSubject(invoice.getEmailSubject())
            .senderEmail(invoice.getSenderEmail())
            .originalFilename(invoice.getOriginalFilename())
            .fileSize(invoice.getFileSize())
            .downloadUrl(invoice.getDownloadUrl())
            .build();
    }
}
