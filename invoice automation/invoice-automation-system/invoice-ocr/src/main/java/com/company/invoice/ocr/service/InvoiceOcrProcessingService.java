package com.company.invoice.ocr.service;

import com.company.invoice.ocr.service.parsing.InvoiceDataExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Main OCR processing service that orchestrates the complete invoice processing pipeline.
 * Integrates text extraction, data parsing, and validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceOcrProcessingService {

    private final TextExtractionCoordinator textExtractionCoordinator;
    private final InvoiceDataExtractor invoiceDataExtractor;

    /**
     * Process invoice PDF through complete OCR pipeline
     */
    public InvoiceProcessingResult processInvoicePdf(byte[] pdfBytes, String filename, 
                                                    String emailSubject, String senderEmail) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 Starting complete invoice OCR processing for: {}", filename);

        InvoiceProcessingResult.InvoiceProcessingResultBuilder builder = InvoiceProcessingResult.builder()
                .filename(filename)
                .emailSubject(emailSubject)
                .senderEmail(senderEmail)
                .processingStartTime(Instant.now())
                .fileSize(pdfBytes.length);

        try {
            // Phase 1: Text Extraction
            log.debug("📄 Phase 1: Text extraction for {}", filename);
            TextExtractionCoordinator.ExtractionCoordinatorResult textResult = 
                    textExtractionCoordinator.extractText(pdfBytes, filename);
            builder.textExtractionResult(textResult);

            if (!textResult.isSuccessful()) {
                log.warn("❌ Text extraction failed for: {} - {}", filename, textResult.getError());
                return builder.status(ProcessingStatus.TEXT_EXTRACTION_FAILED)
                             .error("Text extraction failed: " + textResult.getError())
                             .processingTimeMs(System.currentTimeMillis() - startTime)
                             .build();
            }

            String extractedText = textResult.getBestText();
            double textConfidence = textResult.getBestConfidence();
            
            log.info("✅ Text extraction successful: {} | Method: {} | Confidence: {:.1f}% | Length: {} chars",
                    filename, textResult.getStrategy() != null ? textResult.getStrategy().toString() : "UNKNOWN", 
                    textConfidence * 100, extractedText != null ? extractedText.length() : 0);

            // Phase 2: Data Extraction
            log.debug("🎯 Phase 2: Data extraction for {}", filename);
            InvoiceDataExtractor.InvoiceExtractionResult dataResult = 
                    invoiceDataExtractor.extractInvoiceData(extractedText, filename, emailSubject, senderEmail);
            builder.dataExtractionResult(dataResult);

            if (!dataResult.isSuccessful()) {
                log.warn("❌ Data extraction failed for: {} - Status: {}", filename, dataResult.getStatus());
                return builder.status(ProcessingStatus.DATA_EXTRACTION_FAILED)
                             .error("Data extraction failed: " + dataResult.getStatus())
                             .processingTimeMs(System.currentTimeMillis() - startTime)
                             .build();
            }

            InvoiceDataExtractor.ExtractedInvoiceData extractedData = dataResult.getBestData();
            double dataConfidence = dataResult.getOverallConfidence();

            log.info("✅ Data extraction successful: {} | Fields: {} | Confidence: {:.1f}% | Recommendation: {}",
                    filename, extractedData.getExtractedFieldCount(), dataConfidence * 100, dataResult.getRecommendation());

            // Phase 3: Final Processing Decision
            ProcessingDecision decision = determineProcessingDecision(textConfidence, dataConfidence, dataResult);
            builder.processingDecision(decision);

            // Calculate overall confidence combining text and data confidence
            double overallConfidence = calculateOverallConfidence(textConfidence, dataConfidence);
            builder.overallConfidence(overallConfidence);

            // Determine final status
            ProcessingStatus finalStatus = determineFinalStatus(decision, dataResult);
            builder.status(finalStatus);

            long duration = System.currentTimeMillis() - startTime;
            builder.processingTimeMs(duration);

            log.info("🎉 Complete OCR processing finished: {} | Status: {} | Confidence: {:.1f}% | Decision: {} | Time: {}ms",
                    filename, finalStatus, overallConfidence * 100, decision, duration);

        } catch (Exception e) {
            log.error("❌ Complete OCR processing failed for: {}", filename, e);
            builder.status(ProcessingStatus.PROCESSING_ERROR)
                   .error("Processing error: " + e.getMessage())
                   .processingTimeMs(System.currentTimeMillis() - startTime);
        }

        return builder.build();
    }

    /**
     * Determine processing decision based on confidence scores and validation
     */
    private ProcessingDecision determineProcessingDecision(double textConfidence, double dataConfidence,
                                                          InvoiceDataExtractor.InvoiceExtractionResult dataResult) {
        
        // High confidence across the board → Auto-approve
        if (textConfidence >= 0.9 && dataConfidence >= 0.9 && 
            dataResult.getValidationResult().isValid() &&
            dataResult.getBestData().hasRequiredFields()) {
            return ProcessingDecision.AUTO_APPROVE;
        }

        // Good confidence with minor issues → Review recommended
        if (textConfidence >= 0.8 && dataConfidence >= 0.7 &&
            (dataResult.getValidationResult().isValid() || dataResult.getValidationResult().hasOnlyWarnings())) {
            return ProcessingDecision.REVIEW_RECOMMENDED;
        }

        // Medium confidence or validation errors → Manual review
        if (textConfidence >= 0.6 && dataConfidence >= 0.5) {
            return ProcessingDecision.MANUAL_REVIEW;
        }

        // Low confidence → Manual processing required
        return ProcessingDecision.MANUAL_PROCESSING;
    }

    /**
     * Calculate overall confidence combining text and data extraction confidence
     */
    private double calculateOverallConfidence(double textConfidence, double dataConfidence) {
        // Weighted average: text confidence is 40%, data confidence is 60%
        return (textConfidence * 0.4) + (dataConfidence * 0.6);
    }

    /**
     * Determine final processing status
     */
    private ProcessingStatus determineFinalStatus(ProcessingDecision decision, 
                                                 InvoiceDataExtractor.InvoiceExtractionResult dataResult) {
        switch (decision) {
            case AUTO_APPROVE:
                return ProcessingStatus.READY_FOR_AUTO_PROCESSING;
            case REVIEW_RECOMMENDED:
                return ProcessingStatus.READY_FOR_REVIEW;
            case MANUAL_REVIEW:
                return ProcessingStatus.REQUIRES_MANUAL_REVIEW;
            case MANUAL_PROCESSING:
                return ProcessingStatus.REQUIRES_MANUAL_PROCESSING;
            default:
                return ProcessingStatus.PROCESSING_COMPLETE;
        }
    }

    // Enums and Data Classes

    public enum ProcessingStatus {
        READY_FOR_AUTO_PROCESSING,
        READY_FOR_REVIEW,
        REQUIRES_MANUAL_REVIEW,
        REQUIRES_MANUAL_PROCESSING,
        PROCESSING_COMPLETE,
        TEXT_EXTRACTION_FAILED,
        DATA_EXTRACTION_FAILED,
        PROCESSING_ERROR
    }

    public enum ProcessingDecision {
        AUTO_APPROVE,
        REVIEW_RECOMMENDED,
        MANUAL_REVIEW,
        MANUAL_PROCESSING
    }

    @lombok.Data
    @lombok.Builder
    public static class InvoiceProcessingResult {
        // Input information
        private String filename;
        private String emailSubject;
        private String senderEmail;
        private long fileSize;
        private Instant processingStartTime;

        // Processing results
        private TextExtractionCoordinator.ExtractionCoordinatorResult textExtractionResult;
        private InvoiceDataExtractor.InvoiceExtractionResult dataExtractionResult;

        // Final decision
        private ProcessingDecision processingDecision;
        private ProcessingStatus status;
        private double overallConfidence;
        private long processingTimeMs;
        private String error;

        // Convenience methods
        public boolean isSuccessful() {
            return status != ProcessingStatus.TEXT_EXTRACTION_FAILED &&
                   status != ProcessingStatus.DATA_EXTRACTION_FAILED &&
                   status != ProcessingStatus.PROCESSING_ERROR;
        }

        public boolean requiresManualIntervention() {
            return status == ProcessingStatus.REQUIRES_MANUAL_REVIEW ||
                   status == ProcessingStatus.REQUIRES_MANUAL_PROCESSING;
        }

        public boolean isReadyForAutomation() {
            return status == ProcessingStatus.READY_FOR_AUTO_PROCESSING;
        }

        public String getExtractedText() {
            return textExtractionResult != null ? textExtractionResult.getBestText() : null;
        }

        public InvoiceDataExtractor.ExtractedInvoiceData getExtractedData() {
            return dataExtractionResult != null ? dataExtractionResult.getBestData() : null;
        }

        public String getProcessingSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Status: ").append(status);
            summary.append(" | Confidence: ").append(String.format("%.1f%%", overallConfidence * 100));
            summary.append(" | Decision: ").append(processingDecision);
            
            if (dataExtractionResult != null) {
                InvoiceDataExtractor.ExtractedInvoiceData data = dataExtractionResult.getBestData();
                if (data != null) {
                    summary.append(" | Fields: ").append(data.getExtractedFieldCount());
                }
            }
            
            summary.append(" | Time: ").append(processingTimeMs).append("ms");
            
            return summary.toString();
        }

        public boolean hasError() {
            return error != null && !error.isEmpty();
        }
    }
}
