package com.company.invoice.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Upload response for invoice file uploads
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for invoice upload operations")
public class UploadResponse {

    @Schema(description = "Upload success status", example = "true")
    private boolean success;

    @Schema(description = "Upload result message", example = "Invoice uploaded successfully")
    private String message;

    @Schema(description = "Invoice ID if upload was successful", example = "123")
    private String invoiceId;

    @Schema(description = "Original filename", example = "ACME_Invoice_2025_001.pdf")
    private String fileName;

    @Schema(description = "File size in bytes", example = "245760")
    private Long fileSize;

    @Schema(description = "Processing status", example = "PENDING")
    private String status;

    /**
     * Create successful upload response
     */
    public static UploadResponse success(String invoiceId, String fileName, Long fileSize) {
        return UploadResponse.builder()
            .success(true)
            .message("Invoice uploaded successfully")
            .invoiceId(invoiceId)
            .fileName(fileName)
            .fileSize(fileSize)
            .status("PENDING")
            .build();
    }

    /**
     * Create error upload response
     */
    public static UploadResponse error(String message) {
        return UploadResponse.builder()
            .success(false)
            .message(message)
            .build();
    }
}
