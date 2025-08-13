package com.company.invoice.email.service;

import com.company.invoice.email.entity.Invoice;
import com.company.invoice.email.repository.InvoiceRepository;
import com.company.invoice.ocr.service.InvoiceOcrProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing OCR processing workflows and batch operations.
 * Handles automatic processing, retry logic, and batch updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrProcessingManagementService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceOcrProcessingService ocrProcessingService;
    private final InvoiceDataUpdateService invoiceDataUpdateService;

    /**
     * Process a single invoice through OCR pipeline asynchronously
     */
    @Async("ocrTaskExecutor")
    public CompletableFuture<OcrProcessingResult> processInvoiceAsync(Long invoiceId) {
        log.info("🔍 Starting async OCR processing for invoice: {}", invoiceId);

        try {
            return invoiceRepository.findById(invoiceId)
                    .map(this::processInvoice)
                    .orElse(CompletableFuture.completedFuture(
                            OcrProcessingResult.failure(invoiceId, "Invoice not found")));
        } catch (Exception e) {
            log.error("❌ Exception in async OCR processing for invoice {}", invoiceId, e);
            return CompletableFuture.completedFuture(
                    OcrProcessingResult.failure(invoiceId, "Processing exception: " + e.getMessage()));
        }
    }

    /**
     * Process a single invoice through OCR pipeline synchronously
     */
    public OcrProcessingResult processInvoiceSynchronously(Long invoiceId) {
        log.info("🔍 Starting sync OCR processing for invoice: {}", invoiceId);

        return invoiceRepository.findById(invoiceId)
                .map(this::processInvoiceInternal)
                .orElse(OcrProcessingResult.failure(invoiceId, "Invoice not found"));
    }

    /**
     * Process invoice through complete OCR pipeline
     */
    private CompletableFuture<OcrProcessingResult> processInvoice(Invoice invoice) {
        return CompletableFuture.supplyAsync(() -> processInvoiceInternal(invoice));
    }

    /**
     * Internal method to process invoice through OCR pipeline
     */
    private OcrProcessingResult processInvoiceInternal(Invoice invoice) {
        Long invoiceId = invoice.getId();
        String filename = invoice.getOriginalFilename();

        try {
            // Check if invoice has PDF BLOB
            if (!invoice.hasPdfBlob()) {
                log.warn("⚠️ Invoice {} has no PDF BLOB for OCR processing", invoiceId);
                return OcrProcessingResult.failure(invoiceId, "No PDF BLOB available");
            }

            // Update status to IN_PROGRESS
            updateOcrStatus(invoiceId, Invoice.OcrStatus.IN_PROGRESS, 0.0);

            // Process through OCR pipeline
            InvoiceOcrProcessingService.InvoiceProcessingResult ocrResult = 
                    ocrProcessingService.processInvoicePdf(
                            invoice.getPdfBlob(),
                            filename,
                            invoice.getEmailSubject(),
                            invoice.getSenderEmail()
                    );

            // Update database with results
            return updateInvoiceWithOcrResults(invoice, ocrResult);

        } catch (Exception e) {
            log.error("❌ OCR processing failed for invoice {}", invoiceId, e);
            updateOcrStatus(invoiceId, Invoice.OcrStatus.FAILED, 0.0);
            return OcrProcessingResult.failure(invoiceId, "OCR processing error: " + e.getMessage());
        }
    }

    /**
     * Update invoice with OCR processing results
     */
    private OcrProcessingResult updateInvoiceWithOcrResults(Invoice invoice, 
                                                           InvoiceOcrProcessingService.InvoiceProcessingResult ocrResult) {
        Long invoiceId = invoice.getId();
        
        try {
            // Determine statuses based on OCR results
            Invoice.OcrStatus ocrStatus = ocrResult.isSuccessful() ? Invoice.OcrStatus.COMPLETED : Invoice.OcrStatus.FAILED;
            Invoice.ProcessingStatus processingStatus = determineProcessingStatus(ocrResult);
            BigDecimal confidence = BigDecimal.valueOf(ocrResult.getOverallConfidence());

            // Extract OCR metadata from the result
            String rawText = ocrResult.getExtractedText();
            String ocrMethod = determineOcrMethod(ocrResult);
            Long processingTime = ocrResult.getProcessingTimeMs();
            Integer wordCount = extractWordCount(rawText);
            Integer charCount = rawText != null ? rawText.length() : 0;

            // Create pattern usage data from the extracted data's field extractions
            PatternUsageTracker.PatternUsageData patternUsageData = null;
            if (ocrResult.getExtractedData() != null && 
                ocrResult.getExtractedData().getFieldExtractions() != null &&
                !ocrResult.getExtractedData().getFieldExtractions().isEmpty()) {
                
                patternUsageData = invoiceDataUpdateService.getPatternUsageTracker()
                        .createPatternUsageData(ocrResult.getExtractedData().getFieldExtractions());
            }

            // Update invoice with complete OCR data including raw text and pattern tracking
            InvoiceDataUpdateService.UpdateResult updateResult = 
                    invoiceDataUpdateService.updateInvoiceWithOcrDataAndPatterns(
                            invoiceId,
                            ocrResult.getExtractedData(),
                            ocrStatus,
                            processingStatus,
                            confidence,
                            rawText,
                            ocrMethod,
                            processingTime,
                            wordCount,
                            charCount,
                            patternUsageData // Include pattern tracking data
                    );

            if (updateResult.isSuccessful()) {
                log.info("✅ Successfully updated invoice {} with OCR results: Status={}, Confidence={:.1f}%",
                        invoiceId, ocrStatus, ocrResult.getOverallConfidence() * 100);

                return OcrProcessingResult.success(invoiceId, ocrResult, updateResult);
            } else {
                log.error("❌ Failed to update invoice {} with OCR results: {}", invoiceId, updateResult.getError());
                return OcrProcessingResult.failure(invoiceId, "Database update failed: " + updateResult.getError());
            }

        } catch (Exception e) {
            log.error("❌ Exception updating invoice {} with OCR results", invoiceId, e);
            return OcrProcessingResult.failure(invoiceId, "Update exception: " + e.getMessage());
        }
    }

    /**
     * Batch process multiple invoices that need OCR processing
     */
    public BatchProcessingResult processPendingInvoices(int batchSize) {
        log.info("📦 Starting batch OCR processing for up to {} invoices", batchSize);

        List<Invoice> pendingInvoices = invoiceRepository.findInvoicesNeedingOcrProcessing();
        List<Invoice> toBatch = pendingInvoices.stream()
                .limit(batchSize)
                .toList();

        if (toBatch.isEmpty()) {
            log.info("📭 No invoices found needing OCR processing");
            return BatchProcessingResult.builder()
                    .totalFound(0)
                    .totalProcessed(0)
                    .successful(0)
                    .failed(0)
                    .message("No invoices need processing")
                    .build();
        }

        log.info("📋 Found {} invoices needing OCR processing, processing {} in this batch", 
                pendingInvoices.size(), toBatch.size());

        int successful = 0;
        int failed = 0;

        for (Invoice invoice : toBatch) {
            try {
                OcrProcessingResult result = processInvoiceInternal(invoice);
                if (result.isSuccessful()) {
                    successful++;
                } else {
                    failed++;
                    log.warn("⚠️ OCR processing failed for invoice {}: {}", 
                            invoice.getId(), result.getErrorMessage());
                }
            } catch (Exception e) {
                failed++;
                log.error("❌ Exception processing invoice {} in batch", invoice.getId(), e);
            }
        }

        log.info("📊 Batch OCR processing completed: {} successful, {} failed out of {} total",
                successful, failed, toBatch.size());

        return BatchProcessingResult.builder()
                .totalFound(pendingInvoices.size())
                .totalProcessed(toBatch.size())
                .successful(successful)
                .failed(failed)
                .message(String.format("Processed %d/%d invoices successfully", successful, toBatch.size()))
                .build();
    }

    /**
     * Retry failed OCR processing for specific invoices
     */
    public BatchProcessingResult retryFailedProcessing(List<Long> invoiceIds) {
        log.info("🔄 Retrying OCR processing for {} invoices", invoiceIds.size());

        int successful = 0;
        int failed = 0;

        for (Long invoiceId : invoiceIds) {
            try {
                OcrProcessingResult result = processInvoiceSynchronously(invoiceId);
                if (result.isSuccessful()) {
                    successful++;
                    log.info("✅ Retry successful for invoice {}", invoiceId);
                } else {
                    failed++;
                    log.warn("⚠️ Retry failed for invoice {}: {}", invoiceId, result.getErrorMessage());
                }
            } catch (Exception e) {
                failed++;
                log.error("❌ Exception retrying invoice {}", invoiceId, e);
            }
        }

        log.info("📊 Retry processing completed: {} successful, {} failed", successful, failed);

        return BatchProcessingResult.builder()
                .totalFound(invoiceIds.size())
                .totalProcessed(invoiceIds.size())
                .successful(successful)
                .failed(failed)
                .message(String.format("Retry completed: %d/%d successful", successful, invoiceIds.size()))
                .build();
    }

    /**
     * Get OCR processing statistics
     */
    public OcrStatistics getOcrStatistics() {
        Object[] stats = invoiceRepository.getOcrStatistics();
        
        if (stats != null && stats.length >= 5) {
            return OcrStatistics.builder()
                    .totalInvoices(((Number) stats[0]).longValue())
                    .completedOcr(((Number) stats[1]).longValue())
                    .failedOcr(((Number) stats[2]).longValue())
                    .pendingOcr(((Number) stats[3]).longValue())
                    .averageConfidence(stats[4] != null ? ((Number) stats[4]).doubleValue() : 0.0)
                    .build();
        }

        return OcrStatistics.builder().build();
    }

    /**
     * Find invoices needing OCR processing
     */
    public List<Invoice> getInvoicesNeedingOcrProcessing() {
        return invoiceRepository.findInvoicesNeedingOcrProcessing();
    }

    /**
     * Helper method to update OCR status
     */
    private void updateOcrStatus(Long invoiceId, Invoice.OcrStatus status, Double confidence) {
        try {
            BigDecimal confidenceBd = confidence != null ? BigDecimal.valueOf(confidence) : BigDecimal.ZERO;
            invoiceDataUpdateService.updateOcrStatus(invoiceId, status, confidenceBd);
        } catch (Exception e) {
            log.error("❌ Failed to update OCR status for invoice {}", invoiceId, e);
        }
    }

    /**
     * Determine processing status based on OCR results
     */
    private Invoice.ProcessingStatus determineProcessingStatus(InvoiceOcrProcessingService.InvoiceProcessingResult ocrResult) {
        if (!ocrResult.isSuccessful()) {
            return Invoice.ProcessingStatus.FAILED;
        }

        switch (ocrResult.getStatus()) {
            case READY_FOR_AUTO_PROCESSING:
                return Invoice.ProcessingStatus.COMPLETED;
            case READY_FOR_REVIEW:
            case REQUIRES_MANUAL_REVIEW:
            case REQUIRES_MANUAL_PROCESSING:
                return Invoice.ProcessingStatus.PENDING;
            default:
                return Invoice.ProcessingStatus.FAILED;
        }
    }

    /**
     * Determine OCR method used from the result
     */
    private String determineOcrMethod(InvoiceOcrProcessingService.InvoiceProcessingResult ocrResult) {
        if (ocrResult.getTextExtractionResult() != null) {
            // Try to extract method from the text extraction result strategy
            return ocrResult.getTextExtractionResult().toString().contains("TIKA") ? "TIKA" :
                   ocrResult.getTextExtractionResult().toString().contains("TESSERACT") ? "TESSERACT" :
                   ocrResult.getTextExtractionResult().toString().contains("HYBRID") ? "HYBRID" : "UNKNOWN";
        }
        return "UNKNOWN";
    }

    /**
     * Extract word count from raw text
     */
    private Integer extractWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        // Split by whitespace and count non-empty parts
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    // Data classes

    @lombok.Data
    @lombok.Builder
    public static class OcrProcessingResult {
        private Long invoiceId;
        private boolean successful;
        private InvoiceOcrProcessingService.InvoiceProcessingResult ocrResult;
        private InvoiceDataUpdateService.UpdateResult updateResult;
        private String errorMessage;

        public static OcrProcessingResult success(Long invoiceId, 
                                                 InvoiceOcrProcessingService.InvoiceProcessingResult ocrResult,
                                                 InvoiceDataUpdateService.UpdateResult updateResult) {
            return OcrProcessingResult.builder()
                    .invoiceId(invoiceId)
                    .successful(true)
                    .ocrResult(ocrResult)
                    .updateResult(updateResult)
                    .build();
        }

        public static OcrProcessingResult failure(Long invoiceId, String errorMessage) {
            return OcrProcessingResult.builder()
                    .invoiceId(invoiceId)
                    .successful(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchProcessingResult {
        private int totalFound;
        private int totalProcessed;
        private int successful;
        private int failed;
        private String message;

        public double getSuccessRate() {
            return totalProcessed > 0 ? (double) successful / totalProcessed : 0.0;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class OcrStatistics {
        private long totalInvoices;
        private long completedOcr;
        private long failedOcr;
        private long pendingOcr;
        private double averageConfidence;

        public double getCompletionRate() {
            return totalInvoices > 0 ? (double) completedOcr / totalInvoices : 0.0;
        }

        public double getFailureRate() {
            return totalInvoices > 0 ? (double) failedOcr / totalInvoices : 0.0;
        }
    }
}
