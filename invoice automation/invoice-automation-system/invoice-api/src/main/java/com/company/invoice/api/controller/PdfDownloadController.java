package com.company.invoice.api.controller;

import com.company.invoice.email.entity.Invoice;
import com.company.invoice.email.service.database.PdfDatabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for PDF download operations.
 * Provides secure download links for PDF invoices stored as database BLOBs.
 */
@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PDF Management", description = "PDF download and management operations")
public class PdfDownloadController {

    private final PdfDatabaseService pdfDatabaseService;

    /**
     * Download PDF by secure token
     */
    @GetMapping("/download/{token}")
    @Operation(summary = "Download PDF by secure token", 
               description = "Download PDF invoice using a secure, time-limited download token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "PDF not found or token expired"),
        @ApiResponse(responseCode = "410", description = "Download link expired or exceeded max downloads")
    })
    public ResponseEntity<byte[]> downloadPdf(
            @Parameter(description = "Secure download token", required = true)
            @PathVariable String token) {
        
        try {
            log.info("📥 PDF download request for token: {}", token);
            
            Optional<Invoice> invoiceOpt = pdfDatabaseService.getPdfByDownloadToken(token);
            
            if (invoiceOpt.isEmpty()) {
                log.warn("❌ PDF download failed - invalid or expired token: {}", token);
                return ResponseEntity.notFound().build();
            }
            
            Invoice invoice = invoiceOpt.get();
            
            // Validate PDF BLOB exists
            if (!invoice.hasPdfBlob()) {
                log.error("❌ PDF BLOB missing for invoice: {}", invoice.getId());
                return ResponseEntity.status(HttpStatus.GONE)
                        .build();
            }
            
            // Prepare response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(invoice.getPdfBlobSize());
            
            // Set Content-Disposition for download
            String encodedFilename = URLEncoder.encode(invoice.getOriginalFilename(), StandardCharsets.UTF_8);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + encodedFilename + "\"");
            
            // Add custom headers for tracking
            headers.set("X-Invoice-ID", invoice.getId().toString());
            headers.set("X-Download-Count", invoice.getDownloadCount().toString());
            headers.set("X-File-Size", String.valueOf(invoice.getFileSize()));
            
            log.info("✅ PDF download successful: {} ({} bytes, download #{}/{})", 
                    invoice.getOriginalFilename(), invoice.getFileSize(), 
                    invoice.getDownloadCount(), invoice.getMaxDownloads());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(invoice.getPdfBlob());
                    
        } catch (Exception e) {
            log.error("❌ Unexpected error during PDF download for token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get invoice metadata by token (without downloading)
     */
    @GetMapping("/info/{token}")
    @Operation(summary = "Get PDF info by token", 
               description = "Get PDF metadata without downloading the file")
    public ResponseEntity<PdfInfo> getPdfInfo(
            @Parameter(description = "Secure download token", required = true)
            @PathVariable String token) {
        
        try {
            Optional<Invoice> invoiceOpt = pdfDatabaseService.getPdfByDownloadToken(token);
            
            if (invoiceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Invoice invoice = invoiceOpt.get();
            PdfInfo pdfInfo = new PdfInfo(
                    invoice.getId(),
                    invoice.getOriginalFilename(),
                    invoice.getFileSize(),
                    invoice.getContentType(),
                    invoice.getCreatedAt().toString(),
                    invoice.getSenderEmail(),
                    invoice.getEmailSubject(),
                    invoice.getDownloadCount(),
                    invoice.getMaxDownloads(),
                    invoice.getDownloadExpiresAt().toString(),
                    invoice.getProcessingStatus().toString(),
                    invoice.getOcrStatus().toString()
            );
            
            return ResponseEntity.ok(pdfInfo);
            
        } catch (Exception e) {
            log.error("❌ Error getting PDF info for token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get storage statistics (admin only)
     */
    @GetMapping("/stats")
    @Operation(summary = "Get storage statistics", 
               description = "Get database storage statistics for PDF BLOBs")
    public ResponseEntity<PdfDatabaseService.StorageStats> getStorageStats() {
        try {
            PdfDatabaseService.StorageStats stats = pdfDatabaseService.getStorageStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ Error getting storage stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cleanup expired download links (admin only)
     */
    @PostMapping("/cleanup")
    @Operation(summary = "Cleanup expired links", 
               description = "Clean up expired download links and regenerate tokens")
    public ResponseEntity<CleanupResult> cleanupExpiredLinks() {
        try {
            int cleanedCount = pdfDatabaseService.cleanupExpiredDownloadLinks();
            CleanupResult result = new CleanupResult(cleanedCount, "Cleanup completed successfully");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Error during cleanup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CleanupResult(0, "Cleanup failed: " + e.getMessage()));
        }
    }

    /**
     * PDF information response DTO
     */
    public record PdfInfo(
            Long invoiceId,
            String filename,
            Long fileSize,
            String contentType,
            String createdAt,
            String senderEmail,
            String emailSubject,
            Integer downloadCount,
            Integer maxDownloads,
            String expiresAt,
            String processingStatus,
            String ocrStatus
    ) {}

    /**
     * Cleanup result response DTO
     */
    public record CleanupResult(
            int cleanedCount,
            String message
    ) {}
}
