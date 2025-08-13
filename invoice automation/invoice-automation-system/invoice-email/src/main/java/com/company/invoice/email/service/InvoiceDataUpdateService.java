package com.company.invoice.email.service;

import com.company.invoice.email.entity.Invoice;
import com.company.invoice.email.repository.InvoiceRepository;
import com.company.invoice.common.service.UrlBuilderService;
import com.company.invoice.ocr.service.parsing.InvoiceDataExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Service for updating invoice entities with OCR-extracted data.
 * Provides transactional updates with proper error handling and validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceDataUpdateService {

    private final InvoiceRepository invoiceRepository;
    private final PatternUsageTracker patternUsageTracker;
    private final UrlBuilderService urlBuilderService;
    
    /**
     * Get the pattern usage tracker for external use
     */
    public PatternUsageTracker getPatternUsageTracker() {
        return patternUsageTracker;
    }

    /**
     * Update invoice with complete OCR extraction results
     */
    public UpdateResult updateInvoiceWithOcrData(Long invoiceId, 
                                                InvoiceDataExtractor.ExtractedInvoiceData extractedData,
                                                Invoice.OcrStatus ocrStatus,
                                                Invoice.ProcessingStatus processingStatus,
                                                BigDecimal ocrConfidence,
                                                String rawExtractedText,
                                                String ocrMethod,
                                                Long processingTimeMs,
                                                Integer wordCount,
                                                Integer characterCount) {
        
        log.info("📝 Updating invoice {} with OCR data: {} fields extracted", invoiceId, 
                extractedData != null ? extractedData.getExtractedFieldCount() : 0);

        try {
            // Verify invoice exists
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            if (invoiceOpt.isEmpty()) {
                log.error("❌ Invoice not found for update: {}", invoiceId);
                return UpdateResult.failure("Invoice not found: " + invoiceId);
            }

            // Invoice exists, proceed with update
            
            // Strategy 1: Use custom JPQL update for performance (recommended for production)
            int updatedRows = updateUsingCustomQuery(invoiceId, extractedData, ocrStatus, processingStatus, ocrConfidence,
                    rawExtractedText, ocrMethod, processingTimeMs, wordCount, characterCount);
            
            if (updatedRows > 0) {
                log.info("✅ Successfully updated invoice {} using custom query", invoiceId);
                
                // Log the update details
                logUpdateDetails(invoiceId, extractedData, ocrStatus, processingStatus, ocrConfidence);
                
                return UpdateResult.success("Invoice updated successfully", updatedRows);
            } else {
                log.warn("⚠️ No rows updated for invoice {}", invoiceId);
                return UpdateResult.failure("No rows updated for invoice: " + invoiceId);
            }

        } catch (Exception e) {
            log.error("❌ Failed to update invoice {} with OCR data", invoiceId, e);
            return UpdateResult.failure("Update failed: " + e.getMessage());
        }
    }

    /**
     * Update invoice using custom JPQL query for optimal performance
     */
    private int updateUsingCustomQuery(Long invoiceId, 
                                      InvoiceDataExtractor.ExtractedInvoiceData extractedData,
                                      Invoice.OcrStatus ocrStatus,
                                      Invoice.ProcessingStatus processingStatus,
                                      BigDecimal ocrConfidence,
                                      String rawExtractedText,
                                      String ocrMethod,
                                      Long processingTimeMs,
                                      Integer wordCount,
                                      Integer characterCount) {
        
        // TEMPORARILY use entity approach due to Hibernate JPQL issues
        // Important: Use the current status values passed to this method, not cached entity values
        UpdateResult entityResult = updateInvoiceWithEntityApproach(invoiceId, extractedData, ocrStatus, processingStatus, ocrConfidence);
        
        // Also update raw text separately if provided
        if (rawExtractedText != null) {
            updateRawTextAndMetadata(invoiceId, rawExtractedText, ocrMethod, processingTimeMs, wordCount, characterCount);
        }
        
        return entityResult.isSuccessful() ? 1 : 0;
    }

    /**
     * Update invoice with OCR data and pattern usage tracking
     */
    public UpdateResult updateInvoiceWithOcrDataAndPatterns(
            Long invoiceId, 
            InvoiceDataExtractor.ExtractedInvoiceData extractedData,
            Invoice.OcrStatus ocrStatus,
            Invoice.ProcessingStatus processingStatus,
            BigDecimal ocrConfidence,
            String rawExtractedText,
            String ocrMethod,
            Long processingTimeMs,
            Integer wordCount,
            Integer characterCount,
            PatternUsageTracker.PatternUsageData patternUsageData) {
        
        log.info("📝 Updating invoice {} with OCR data and pattern tracking: {} fields extracted", 
                invoiceId, extractedData != null ? extractedData.getExtractedFieldCount() : 0);

        try {
            // First, update the invoice with OCR data
            UpdateResult result = updateInvoiceWithOcrData(invoiceId, extractedData, ocrStatus, 
                    processingStatus, ocrConfidence, rawExtractedText, ocrMethod, 
                    processingTimeMs, wordCount, characterCount);

            if (result.isSuccessful() && patternUsageData != null) {
                // Update pattern tracking information
                updatePatternTrackingData(invoiceId, patternUsageData);
            }

            return result;

        } catch (Exception e) {
            log.error("❌ Failed to update invoice {} with OCR data and patterns", invoiceId, e);
            return UpdateResult.failure("Update with pattern tracking failed: " + e.getMessage());
        }
    }

    /**
     * Update pattern tracking data for an invoice
     */
    private void updatePatternTrackingData(Long invoiceId, PatternUsageTracker.PatternUsageData patternUsageData) {
        try {
            int updatedRows = invoiceRepository.updatePatternTrackingData(
                    invoiceId,
                    patternUsageData.getUsedPatternIds(),
                    patternUsageData.getPatternMatchSummary(),
                    patternUsageData.getExtractionConfidenceDetails()
            );

            if (updatedRows > 0) {
                log.debug("✅ Pattern tracking data updated for invoice {}", invoiceId);
            } else {
                log.warn("⚠️ No rows updated when setting pattern tracking for invoice {}", invoiceId);
            }

        } catch (Exception e) {
            log.error("❌ Failed to update pattern tracking for invoice {}", invoiceId, e);
            // Don't fail the whole operation for pattern tracking issues
        }
    }

    /**
     * Generate and update download URL for an invoice
     */
    public UpdateResult updateDownloadUrl(Long invoiceId) {
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            if (invoiceOpt.isEmpty()) {
                return UpdateResult.failure("Invoice not found: " + invoiceId);
            }

            Invoice invoice = invoiceOpt.get();
            
            // Generate download URL using the UrlBuilderService
            String downloadUrl = urlBuilderService.buildDownloadUrl(invoice.getDownloadToken());
            
            if (downloadUrl == null) {
                return UpdateResult.failure("Failed to generate download URL - invalid token");
            }
            
            int updatedRows = invoiceRepository.updateDownloadUrl(invoiceId, downloadUrl);
            
            if (updatedRows > 0) {
                log.info("✅ Download URL updated for invoice {}: {}", invoiceId, downloadUrl);
                return UpdateResult.success("Download URL updated successfully", updatedRows);
            } else {
                return UpdateResult.failure("Failed to update download URL");
            }

        } catch (Exception e) {
            log.error("❌ Failed to update download URL for invoice {}", invoiceId, e);
            return UpdateResult.failure("Failed to update download URL: " + e.getMessage());
        }
    }

    /**
     * Update invoice using JPA entity approach (alternative method)
     */
    public UpdateResult updateInvoiceWithEntityApproach(Long invoiceId, 
                                                       InvoiceDataExtractor.ExtractedInvoiceData extractedData,
                                                       Invoice.OcrStatus ocrStatus,
                                                       Invoice.ProcessingStatus processingStatus,
                                                       BigDecimal ocrConfidence) {
        
        log.debug("📝 Updating invoice {} using entity approach", invoiceId);

        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            if (invoiceOpt.isEmpty()) {
                return UpdateResult.failure("Invoice not found: " + invoiceId);
            }

            Invoice invoice = invoiceOpt.get();
            
            // Update extracted data if available
            if (extractedData != null) {
                updateInvoiceFields(invoice, extractedData);
            }
            
            // Update OCR status and confidence
            invoice.setOcrStatus(ocrStatus);
            invoice.setOcrConfidence(ocrConfidence);
            invoice.setProcessingStatus(processingStatus);

            // Save the updated entity
            Invoice savedInvoice = invoiceRepository.save(invoice);
            
            log.info("✅ Successfully updated invoice {} using entity approach", savedInvoice.getId());
            logUpdateDetails(invoiceId, extractedData, ocrStatus, processingStatus, ocrConfidence);
            
            return UpdateResult.success("Invoice updated successfully (entity approach)", 1);

        } catch (Exception e) {
            log.error("❌ Failed to update invoice {} using entity approach", invoiceId, e);
            return UpdateResult.failure("Entity update failed: " + e.getMessage());
        }
    }

    /**
     * Update specific invoice fields with extracted data
     */
    private void updateInvoiceFields(Invoice invoice, InvoiceDataExtractor.ExtractedInvoiceData extractedData) {
        // Only update fields that were successfully extracted (not null)
        if (extractedData.getInvoiceNumber() != null && !extractedData.getInvoiceNumber().trim().isEmpty()) {
            invoice.setInvoiceNumber(extractedData.getInvoiceNumber().trim());
        }
        
        if (extractedData.getVendorName() != null && !extractedData.getVendorName().trim().isEmpty()) {
            invoice.setVendorName(extractedData.getVendorName().trim());
        }
        
        if (extractedData.getTotalAmount() != null && extractedData.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            invoice.setTotalAmount(extractedData.getTotalAmount());
        }
        
        if (extractedData.getInvoiceDate() != null) {
            invoice.setInvoiceDate(extractedData.getInvoiceDate());
        }
        
        if (extractedData.getDueDate() != null) {
            invoice.setDueDate(extractedData.getDueDate());
        }
        
        if (extractedData.getCurrency() != null && !extractedData.getCurrency().trim().isEmpty()) {
            invoice.setCurrency(extractedData.getCurrency().trim().toUpperCase());
        }
    }

    /**
     * Update only OCR status and confidence
     */
    public UpdateResult updateOcrStatus(Long invoiceId, Invoice.OcrStatus ocrStatus, BigDecimal ocrConfidence) {
        log.debug("📊 Updating OCR status for invoice {}: {} (confidence: {}%)", 
                 invoiceId, ocrStatus, ocrConfidence.multiply(BigDecimal.valueOf(100)));

        try {
            int updatedRows = invoiceRepository.updateOcrStatus(invoiceId, ocrStatus, ocrConfidence);
            
            if (updatedRows > 0) {
                log.debug("✅ OCR status updated for invoice {}", invoiceId);
                return UpdateResult.success("OCR status updated", updatedRows);
            } else {
                log.warn("⚠️ No rows updated when setting OCR status for invoice {}", invoiceId);
                return UpdateResult.failure("No rows updated for OCR status");
            }

        } catch (Exception e) {
            log.error("❌ Failed to update OCR status for invoice {}", invoiceId, e);
            return UpdateResult.failure("OCR status update failed: " + e.getMessage());
        }
    }

    /**
     * Update only processing status
     */
    public UpdateResult updateProcessingStatus(Long invoiceId, Invoice.ProcessingStatus processingStatus) {
        log.debug("📋 Updating processing status for invoice {}: {}", invoiceId, processingStatus);

        try {
            int updatedRows = invoiceRepository.updateProcessingStatus(invoiceId, processingStatus);
            
            if (updatedRows > 0) {
                log.debug("✅ Processing status updated for invoice {}", invoiceId);
                return UpdateResult.success("Processing status updated", updatedRows);
            } else {
                log.warn("⚠️ No rows updated when setting processing status for invoice {}", invoiceId);
                return UpdateResult.failure("No rows updated for processing status");
            }

        } catch (Exception e) {
            log.error("❌ Failed to update processing status for invoice {}", invoiceId, e);
            return UpdateResult.failure("Processing status update failed: " + e.getMessage());
        }
    }

    /**
     * Update basic invoice data (invoice number, vendor, amount)
     */
    public UpdateResult updateBasicData(Long invoiceId, String invoiceNumber, String vendorName, 
                                       BigDecimal totalAmount, String currency) {
        log.debug("📋 Updating basic data for invoice {}: InvoiceNum={}, Vendor={}, Amount={}", 
                 invoiceId, invoiceNumber, vendorName, totalAmount);

        try {
            int updatedRows = invoiceRepository.updateBasicInvoiceData(
                invoiceId, invoiceNumber, vendorName, totalAmount, currency);
            
            if (updatedRows > 0) {
                log.debug("✅ Basic data updated for invoice {}", invoiceId);
                return UpdateResult.success("Basic data updated", updatedRows);
            } else {
                log.warn("⚠️ No rows updated when setting basic data for invoice {}", invoiceId);
                return UpdateResult.failure("No rows updated for basic data");
            }

        } catch (Exception e) {
            log.error("❌ Failed to update basic data for invoice {}", invoiceId, e);
            return UpdateResult.failure("Basic data update failed: " + e.getMessage());
        }
    }

    /**
     * Update invoice dates
     */
    public UpdateResult updateDates(Long invoiceId, LocalDate invoiceDate, LocalDate dueDate) {
        log.debug("📅 Updating dates for invoice {}: InvoiceDate={}, DueDate={}", 
                 invoiceId, invoiceDate, dueDate);

        try {
            int updatedRows = invoiceRepository.updateInvoiceDates(invoiceId, invoiceDate, dueDate);
            
            if (updatedRows > 0) {
                log.debug("✅ Dates updated for invoice {}", invoiceId);
                return UpdateResult.success("Dates updated", updatedRows);
            } else {
                log.warn("⚠️ No rows updated when setting dates for invoice {}", invoiceId);
                return UpdateResult.failure("No rows updated for dates");
            }

        } catch (Exception e) {
            log.error("❌ Failed to update dates for invoice {}", invoiceId, e);
            return UpdateResult.failure("Date update failed: " + e.getMessage());
        }
    }

    /**
     * Batch update OCR status for multiple invoices
     */
    public UpdateResult batchUpdateOcrStatus(java.util.List<Long> invoiceIds, Invoice.OcrStatus ocrStatus) {
        log.info("📦 Batch updating OCR status for {} invoices: {}", invoiceIds.size(), ocrStatus);

        try {
            int updatedRows = invoiceRepository.batchUpdateOcrStatus(invoiceIds, ocrStatus);
            
            log.info("✅ Batch OCR status update completed: {} invoices updated", updatedRows);
            return UpdateResult.success("Batch OCR status updated", updatedRows);

        } catch (Exception e) {
            log.error("❌ Failed to batch update OCR status for {} invoices", invoiceIds.size(), e);
            return UpdateResult.failure("Batch update failed: " + e.getMessage());
        }
    }

    /**
     * Update raw text and OCR metadata only
     */
    public UpdateResult updateRawTextAndMetadata(Long invoiceId, String rawExtractedText, 
                                                String ocrMethod, Long processingTimeMs,
                                                Integer wordCount, Integer characterCount) {
        log.debug("📄 Updating raw text and metadata for invoice {}", invoiceId);

        try {
            int updatedRows = invoiceRepository.updateRawTextAndMetadata(
                invoiceId, rawExtractedText, ocrMethod, 
                processingTimeMs != null ? processingTimeMs.intValue() : null,
                wordCount, characterCount);
            
            if (updatedRows > 0) {
                log.debug("✅ Raw text and metadata updated for invoice {}", invoiceId);
                return UpdateResult.success("Raw text and metadata updated", updatedRows);
            } else {
                log.warn("⚠️ No rows updated when setting raw text for invoice {}", invoiceId);
                return UpdateResult.failure("No rows updated for raw text");
            }

        } catch (Exception e) {
            log.error("❌ Failed to update raw text for invoice {}", invoiceId, e);
            return UpdateResult.failure("Raw text update failed: " + e.getMessage());
        }
    }

    /**
     * Get invoice by ID for verification
     */
    public Optional<Invoice> getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    /**
     * Log detailed update information
     */
    private void logUpdateDetails(Long invoiceId, 
                                 InvoiceDataExtractor.ExtractedInvoiceData extractedData,
                                 Invoice.OcrStatus ocrStatus,
                                 Invoice.ProcessingStatus processingStatus,
                                 BigDecimal ocrConfidence) {
        
        if (extractedData != null) {
            log.debug("📊 Invoice {} update details:", invoiceId);
            log.debug("  📄 Invoice Number: {}", extractedData.getInvoiceNumber());
            log.debug("  🏢 Vendor Name: {}", extractedData.getVendorName());
            log.debug("  💰 Total Amount: {} {}", extractedData.getTotalAmount(), extractedData.getCurrency());
            log.debug("  📅 Invoice Date: {}", extractedData.getInvoiceDate());
            log.debug("  ⏰ Due Date: {}", extractedData.getDueDate());
            log.debug("  🔍 OCR Status: {} (confidence: {}%)", ocrStatus, 
                     ocrConfidence != null ? ocrConfidence.multiply(BigDecimal.valueOf(100)) : "N/A");
            log.debug("  📋 Processing Status: {}", processingStatus);
        }
    }

    // Result classes

    @lombok.Data
    @lombok.Builder
    public static class UpdateResult {
        private boolean success;
        private String message;
        private int rowsAffected;
        private String error;

        public static UpdateResult success(String message, int rowsAffected) {
            return UpdateResult.builder()
                    .success(true)
                    .message(message)
                    .rowsAffected(rowsAffected)
                    .build();
        }

        public static UpdateResult failure(String error) {
            return UpdateResult.builder()
                    .success(false)
                    .error(error)
                    .rowsAffected(0)
                    .build();
        }

        public boolean isSuccessful() {
            return success && rowsAffected > 0;
        }
    }
}
