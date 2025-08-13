package com.company.invoice.api.controller;

import com.company.invoice.data.entity.InvoicePattern;
import com.company.invoice.data.repository.InvoicePatternRepository;
import com.company.invoice.ocr.service.parsing.DatabasePatternLibrary;
import com.company.invoice.email.entity.Invoice;
import com.company.invoice.email.service.OcrProcessingManagementService;
import com.company.invoice.email.repository.InvoiceRepository;
import com.company.invoice.ocr.service.debugging.TextExtractionDebugger;

import java.math.BigDecimal;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for OCR processing management operations.
 * Provides endpoints for triggering, monitoring, and managing OCR processing.
 */
@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OCR Management", description = "OCR processing management and monitoring operations")
public class OcrManagementController {

    private final OcrProcessingManagementService ocrManagementService;
    private final TextExtractionDebugger textExtractionDebugger;
    private final InvoiceRepository invoiceRepository;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.company.invoice.email.scheduler.EmailPollingScheduler emailPollingScheduler;

    /**
     * Process a single invoice through OCR pipeline
     */
    @PostMapping("/process/{invoiceId}")
    @Operation(summary = "Process invoice OCR", 
               description = "Trigger OCR processing for a specific invoice")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OCR processing initiated successfully"),
        @ApiResponse(responseCode = "404", description = "Invoice not found"),
        @ApiResponse(responseCode = "400", description = "Invalid invoice ID or processing error")
    })
    public ResponseEntity<OcrProcessingResponse> processInvoice(
            @Parameter(description = "Invoice ID to process", required = true)
            @PathVariable Long invoiceId) {
        
        log.info("🔍 OCR processing requested for invoice: {}", invoiceId);
        
        try {
            OcrProcessingManagementService.OcrProcessingResult result = 
                    ocrManagementService.processInvoiceSynchronously(invoiceId);
            
            if (result.isSuccessful()) {
                return ResponseEntity.ok(OcrProcessingResponse.success(
                    "OCR processing completed successfully", result));
            } else {
                return ResponseEntity.badRequest().body(OcrProcessingResponse.failure(
                    "OCR processing failed: " + result.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("❌ Error processing OCR for invoice {}", invoiceId, e);
            return ResponseEntity.internalServerError().body(OcrProcessingResponse.failure(
                "OCR processing error: " + e.getMessage()));
        }
    }

    /**
     * Process a single invoice asynchronously
     */
    @PostMapping("/process-async/{invoiceId}")
    @Operation(summary = "Process invoice OCR asynchronously", 
               description = "Trigger asynchronous OCR processing for a specific invoice")
    public ResponseEntity<AsyncProcessingResponse> processInvoiceAsync(
            @Parameter(description = "Invoice ID to process", required = true)
            @PathVariable Long invoiceId) {
        
        log.info("🔍 Async OCR processing requested for invoice: {}", invoiceId);
        
        try {
            // Initiate async processing (result will be handled asynchronously)
            ocrManagementService.processInvoiceAsync(invoiceId);
            
            return ResponseEntity.ok(AsyncProcessingResponse.builder()
                    .message("OCR processing initiated asynchronously")
                    .invoiceId(invoiceId)
                    .status("PROCESSING")
                    .build());
            
        } catch (Exception e) {
            log.error("❌ Error initiating async OCR for invoice {}", invoiceId, e);
            return ResponseEntity.internalServerError().body(AsyncProcessingResponse.builder()
                    .message("Failed to initiate OCR processing: " + e.getMessage())
                    .invoiceId(invoiceId)
                    .status("ERROR")
                    .build());
        }
    }

    /**
     * Batch process pending invoices
     */
    @PostMapping("/batch-process")
    @Operation(summary = "Batch process pending invoices", 
               description = "Process multiple invoices that are pending OCR processing")
    public ResponseEntity<BatchProcessingResponse> batchProcessPendingInvoices(
            @Parameter(description = "Maximum number of invoices to process in this batch")
            @RequestParam(defaultValue = "10") int batchSize) {
        
        log.info("📦 Batch OCR processing requested for up to {} invoices", batchSize);
        
        try {
            OcrProcessingManagementService.BatchProcessingResult result = 
                    ocrManagementService.processPendingInvoices(batchSize);
            
            return ResponseEntity.ok(BatchProcessingResponse.builder()
                    .message(result.getMessage())
                    .totalFound(result.getTotalFound())
                    .totalProcessed(result.getTotalProcessed())
                    .successful(result.getSuccessful())
                    .failed(result.getFailed())
                    .successRate(result.getSuccessRate())
                    .build());
            
        } catch (Exception e) {
            log.error("❌ Error in batch OCR processing", e);
            return ResponseEntity.internalServerError().body(BatchProcessingResponse.builder()
                    .message("Batch processing failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Retry failed OCR processing
     */
    @PostMapping("/retry")
    @Operation(summary = "Retry failed OCR processing", 
               description = "Retry OCR processing for specific invoices that previously failed")
    public ResponseEntity<BatchProcessingResponse> retryFailedProcessing(
            @Parameter(description = "List of invoice IDs to retry", required = true)
            @RequestBody List<Long> invoiceIds) {
        
        log.info("🔄 OCR retry requested for {} invoices", invoiceIds.size());
        
        try {
            OcrProcessingManagementService.BatchProcessingResult result = 
                    ocrManagementService.retryFailedProcessing(invoiceIds);
            
            return ResponseEntity.ok(BatchProcessingResponse.builder()
                    .message(result.getMessage())
                    .totalFound(result.getTotalFound())
                    .totalProcessed(result.getTotalProcessed())
                    .successful(result.getSuccessful())
                    .failed(result.getFailed())
                    .successRate(result.getSuccessRate())
                    .build());
            
        } catch (Exception e) {
            log.error("❌ Error in OCR retry processing", e);
            return ResponseEntity.internalServerError().body(BatchProcessingResponse.builder()
                    .message("Retry processing failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get OCR processing statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get OCR statistics", 
               description = "Get overall OCR processing statistics and metrics")
    public ResponseEntity<OcrStatisticsResponse> getOcrStatistics() {
        try {
            OcrProcessingManagementService.OcrStatistics stats = 
                    ocrManagementService.getOcrStatistics();
            
            return ResponseEntity.ok(OcrStatisticsResponse.builder()
                    .totalInvoices(stats.getTotalInvoices())
                    .completedOcr(stats.getCompletedOcr())
                    .failedOcr(stats.getFailedOcr())
                    .pendingOcr(stats.getPendingOcr())
                    .averageConfidence(stats.getAverageConfidence())
                    .completionRate(stats.getCompletionRate())
                    .failureRate(stats.getFailureRate())
                    .build());
            
        } catch (Exception e) {
            log.error("❌ Error getting OCR statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get invoices needing OCR processing
     */
    @GetMapping("/pending")
    @Operation(summary = "Get invoices needing OCR processing", 
               description = "List invoices that are pending or failed OCR processing")
    public ResponseEntity<PendingInvoicesResponse> getPendingInvoices() {
        try {
            List<Invoice> pendingInvoices = ocrManagementService.getInvoicesNeedingOcrProcessing();
            
            List<InvoiceSummary> summaries = pendingInvoices.stream()
                    .map(this::createInvoiceSummary)
                    .toList();
            
            return ResponseEntity.ok(PendingInvoicesResponse.builder()
                    .totalCount(summaries.size())
                    .invoices(summaries)
                    .build());
            
        } catch (Exception e) {
            log.error("❌ Error getting pending invoices", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create invoice summary for API response
     */
    private InvoiceSummary createInvoiceSummary(Invoice invoice) {
        return InvoiceSummary.builder()
                .id(invoice.getId())
                .filename(invoice.getOriginalFilename())
                .senderEmail(invoice.getSenderEmail())
                .emailSubject(invoice.getEmailSubject())
                .ocrStatus(invoice.getOcrStatus().toString())
                .processingStatus(invoice.getProcessingStatus().toString())
                .createdAt(invoice.getCreatedAt().toString())
                .fileSize(invoice.getFileSize())
                .build();
    }

    /**
     * Debug text extraction for a specific PDF file (for troubleshooting)
     */
    @PostMapping("/debug/extract-text")
    @Operation(summary = "Debug text extraction", 
               description = "Debug text extraction from a specific PDF file path")
    public ResponseEntity<String> debugTextExtraction(
            @Parameter(description = "Full path to PDF file", required = true)
            @RequestParam String filePath) {
        
        log.info("🔍 Text extraction debug requested for: {}", filePath);
        
        try {
            textExtractionDebugger.debugPdfTextExtraction(filePath);
            return ResponseEntity.ok("Debug analysis completed. Check application logs for detailed results.");
            
        } catch (Exception e) {
            log.error("❌ Error during debug text extraction", e);
            return ResponseEntity.internalServerError().body("Debug failed: " + e.getMessage());
        }
    }

    /**
     * Check scheduler health status
     */
    @GetMapping("/scheduler/status")
    @Operation(summary = "Get scheduler status", 
               description = "Check the health and status of the email polling scheduler")
    public ResponseEntity<SchedulerStatusResponse> getSchedulerStatus() {
        
        log.info("📊 Scheduler status requested");
        
        try {
            // Simple scheduler status check
            String currentTime = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            
            boolean schedulerBeanExists = emailPollingScheduler != null;
            String message = schedulerBeanExists ? 
                "Scheduler bean found. Check logs for HEARTBEAT and TRIGGER messages." :
                "Scheduler bean NOT FOUND! This indicates a configuration issue.";
                    
            return ResponseEntity.ok(SchedulerStatusResponse.builder()
                    .schedulerEnabled(schedulerBeanExists)
                    .currentTime(currentTime)
                    .emailPollingInterval("2 minutes")
                    .heartbeatInterval("30 seconds")
                    .status(schedulerBeanExists ? "CONFIGURED" : "MISSING")
                    .message(message)
                    .build());
            
        } catch (Exception e) {
            log.error("❌ Error getting scheduler status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Force a manual heartbeat test
     */
    @PostMapping("/scheduler/test-heartbeat")
    @Operation(summary = "Test scheduler heartbeat", 
               description = "Manually trigger a heartbeat to test if scheduler is working")
    public ResponseEntity<String> testHeartbeat() {
        
        log.info("🧪 Manual heartbeat test requested");
        
        try {
            if (emailPollingScheduler == null) {
                return ResponseEntity.badRequest().body("❌ Scheduler bean not found! Check configuration.");
            }
            
            // Manually call heartbeat method
            emailPollingScheduler.heartbeat();
            
            return ResponseEntity.ok("✅ Manual heartbeat triggered successfully. Check logs for HEARTBEAT message.");
            
        } catch (Exception e) {
            log.error("❌ Error during manual heartbeat test", e);
            return ResponseEntity.internalServerError().body("❌ Heartbeat test failed: " + e.getMessage());
        }
    }

    /**
     * Test pattern matching with sample text
     */
    @PostMapping("/test/pattern-matching")
    @Operation(summary = "Test pattern matching", 
               description = "Test invoice pattern matching with sample text")
    public ResponseEntity<String> testPatternMatching(
            @io.swagger.v3.oas.annotations.Parameter(description = "Sample text to test")
            @org.springframework.web.bind.annotation.RequestParam String sampleText) {
        
        log.info("🧪 Pattern matching test requested");
        
        try {
            // Test with InvoicePatternLibrary
            com.company.invoice.ocr.service.parsing.InvoicePatternLibrary patternLibrary = 
                new com.company.invoice.ocr.service.parsing.InvoicePatternLibrary();
            
            StringBuilder result = new StringBuilder();
            result.append("=== PATTERN MATCHING TEST RESULTS ===\n");
            result.append("Input text length: ").append(sampleText.length()).append(" characters\n\n");
            
            // Test each pattern type
            result.append("INVOICE NUMBER: ").append(patternLibrary.extractInvoiceNumber(sampleText)).append("\n");
            result.append("TOTAL AMOUNT: ").append(patternLibrary.extractTotalAmount(sampleText)).append("\n");
            result.append("VENDOR NAME: ").append(patternLibrary.extractVendorName(sampleText)).append("\n");
            result.append("INVOICE DATE: ").append(patternLibrary.extractInvoiceDate(sampleText)).append("\n");
            result.append("DUE DATE: ").append(patternLibrary.extractDueDate(sampleText)).append("\n");
            
            result.append("\n=== SAMPLE TEXT (first 500 chars) ===\n");
            result.append(sampleText.length() > 500 ? sampleText.substring(0, 500) + "..." : sampleText);
            
            return ResponseEntity.ok(result.toString());
            
        } catch (Exception e) {
            log.error("❌ Error during pattern matching test", e);
            return ResponseEntity.internalServerError().body("❌ Pattern test failed: " + e.getMessage());
        }
    }

    /**
     * Get raw extracted text for an invoice
     */
    @GetMapping("/raw-text/{invoiceId}")
    @Operation(summary = "Get raw extracted text", 
               description = "Retrieve the raw OCR-extracted text for a specific invoice")
    public ResponseEntity<RawTextResponse> getRawText(
            @Parameter(description = "Invoice ID", required = true)
            @PathVariable Long invoiceId) {
        
        log.info("📄 Raw text requested for invoice: {}", invoiceId);
        
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            
            if (invoiceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Invoice invoice = invoiceOpt.get();
            
            return ResponseEntity.ok(RawTextResponse.builder()
                    .invoiceId(invoiceId)
                    .filename(invoice.getOriginalFilename())
                    .rawExtractedText(invoice.getRawExtractedText())
                    .ocrMethod(invoice.getOcrMethod())
                    .ocrStatus(invoice.getOcrStatus().toString())
                    .processingTimeMs(invoice.getOcrProcessingTimeMs())
                    .wordCount(invoice.getTextWordCount())
                    .characterCount(invoice.getTextCharacterCount())
                    .ocrConfidence(invoice.getOcrConfidence())
                    .build());
            
        } catch (Exception e) {
            log.error("❌ Error getting raw text for invoice {}", invoiceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Response DTOs

    @lombok.Data
    @lombok.Builder
    public static class OcrProcessingResponse {
        private boolean success;
        private String message;
        private Long invoiceId;
        private String processingStatus;
        private Double confidence;

        public static OcrProcessingResponse success(String message, 
                                                   OcrProcessingManagementService.OcrProcessingResult result) {
            return OcrProcessingResponse.builder()
                    .success(true)
                    .message(message)
                    .invoiceId(result.getInvoiceId())
                    .processingStatus(result.getOcrResult() != null ? 
                                    result.getOcrResult().getStatus().toString() : "COMPLETED")
                    .confidence(result.getOcrResult() != null ? 
                              result.getOcrResult().getOverallConfidence() : null)
                    .build();
        }

        public static OcrProcessingResponse failure(String message) {
            return OcrProcessingResponse.builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class AsyncProcessingResponse {
        private String message;
        private Long invoiceId;
        private String status;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchProcessingResponse {
        private String message;
        private int totalFound;
        private int totalProcessed;
        private int successful;
        private int failed;
        private double successRate;
    }

    @lombok.Data
    @lombok.Builder
    public static class OcrStatisticsResponse {
        private long totalInvoices;
        private long completedOcr;
        private long failedOcr;
        private long pendingOcr;
        private double averageConfidence;
        private double completionRate;
        private double failureRate;
    }

    @lombok.Data
    @lombok.Builder
    public static class PendingInvoicesResponse {
        private int totalCount;
        private List<InvoiceSummary> invoices;
    }

    @lombok.Data
    @lombok.Builder
    public static class InvoiceSummary {
        private Long id;
        private String filename;
        private String senderEmail;
        private String emailSubject;
        private String ocrStatus;
        private String processingStatus;
        private String createdAt;
        private Long fileSize;
    }

    @lombok.Data
    @lombok.Builder
    public static class RawTextResponse {
        private Long invoiceId;
        private String filename;
        private String rawExtractedText;
        private String ocrMethod;
        private String ocrStatus;
        private Integer processingTimeMs;
        private Integer wordCount;
        private Integer characterCount;
        private BigDecimal ocrConfidence;
    }

    @lombok.Data
    @lombok.Builder
    public static class SchedulerStatusResponse {
        private boolean schedulerEnabled;
        private String currentTime;
        private String emailPollingInterval;
        private String heartbeatInterval;
        private String status;
        private String message;
    }
}
