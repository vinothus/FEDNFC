package com.company.invoice.email.scheduler;

import com.company.invoice.email.service.EmailMonitoringService;
import com.company.invoice.email.service.EmailMonitoringService.EmailMessage;
import com.company.invoice.email.service.EmailMonitoringService.PdfAttachment;
import com.company.invoice.email.service.PdfStorageService;
import com.company.invoice.email.service.database.PdfDatabaseService;
import com.company.invoice.email.entity.Invoice;
import com.company.invoice.ocr.service.InvoiceOcrProcessingService;
import com.company.invoice.ocr.service.parsing.InvoiceDataExtractor;
import com.company.invoice.email.service.InvoiceDataUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Spring Scheduler service for periodic email monitoring and PDF attachment extraction.
 * 
 * This service runs periodically to:
 * 1. Connect to email server via IMAP
 * 2. Fetch unread emails with PDF attachments
 * 3. Extract PDF attachments
 * 4. Save PDFs to local storage
 * 5. Trigger OCR processing pipeline
 * 6. Mark emails as processed
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "invoice.email.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class EmailPollingScheduler {

    private static final String SCHEDULER_NAME = "EmailPollingScheduler";
    private volatile boolean isSchedulerHealthy = true;
    private volatile long lastSuccessfulRun = 0;
    private volatile long totalRunCount = 0;
    private volatile long errorCount = 0;

    private final EmailMonitoringService emailMonitoringService;
    private final PdfStorageService pdfStorageService;
    private final PdfDatabaseService pdfDatabaseService;
    private final InvoiceOcrProcessingService ocrProcessingService;
    private final InvoiceDataUpdateService invoiceDataUpdateService;

    /**
     * Initialize scheduler on startup
     */
    @jakarta.annotation.PostConstruct
    public void initializeScheduler() {
        log.info("INIT [{}] EmailPollingScheduler starting up...", SCHEDULER_NAME);
        log.info("INIT [{}] Email polling interval: 2 minutes", SCHEDULER_NAME);
        log.info("INIT [{}] Heartbeat interval: 30 seconds", SCHEDULER_NAME);
        log.info("INIT [{}] Scheduler initialized successfully at {}", SCHEDULER_NAME, getCurrentTimestamp());
    }

    /**
     * Scheduled task to poll emails for PDF invoice attachments.
     * 
     * Polling interval is configured via: invoice.email.monitoring.poll-interval
     * - Development: 2 minutes (120 seconds)
     * - Production: 2 minutes (120 seconds)
     */
    @Scheduled(fixedDelayString = "${invoice.email.monitoring.poll-interval:120000}")
    public void pollEmailsForInvoices() {
        long startTime = System.currentTimeMillis();
        totalRunCount++;
        
        log.info("TRIGGER [{}] === {} TRIGGER #{} START === Current Time: {}", 
                SCHEDULER_NAME, SCHEDULER_NAME, totalRunCount, getCurrentTimestamp());
        log.info("STARTING [{}] Starting email polling cycle for invoice PDFs... (Last successful run: {} ago)", 
                SCHEDULER_NAME, getTimeSinceLastRun());
        
        try {
            // Log system health before processing
            logSchedulerHealth();
            
            // 1. Connect to IMAP server
            log.info("📡 [{}] Step 1: Attempting to connect to email server at {}", 
                    SCHEDULER_NAME, getCurrentTimestamp());
            if (!emailMonitoringService.connectToEmailServer()) {
                log.error("❌ [{}] FAILED to connect to email server at {}. Skipping this polling cycle.", 
                        SCHEDULER_NAME, getCurrentTimestamp());
                errorCount++;
                isSchedulerHealthy = false;
                return;
            }
            log.info("✅ [{}] Connected to email server successfully at {}", 
                    SCHEDULER_NAME, getCurrentTimestamp());
            
            // 2. Search for unread emails with PDF attachments
            log.info("📬 [{}] Step 2: Fetching unread emails with PDF attachments at {}", 
                    SCHEDULER_NAME, getCurrentTimestamp());
            List<EmailMonitoringService.EmailMessage> emailsWithPDFs = 
                emailMonitoringService.fetchUnreadEmailsWithPDFs();
            
            if (emailsWithPDFs.isEmpty()) {
                log.info("📭 [{}] No new emails with PDF attachments found at {}", 
                        SCHEDULER_NAME, getCurrentTimestamp());
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ [{}] Email polling cycle completed - no emails to process (took {}ms)", 
                        SCHEDULER_NAME, duration);
                lastSuccessfulRun = System.currentTimeMillis();
                isSchedulerHealthy = true;
                return;
            }
            
            log.info("📬 [{}] Found {} emails with PDF attachments to process at {}", 
                    SCHEDULER_NAME, emailsWithPDFs.size(), getCurrentTimestamp());
            
            // 3. Process each email
            int successCount = 0;
            int failureCount = 0;
            
            for (int i = 0; i < emailsWithPDFs.size(); i++) {
                EmailMonitoringService.EmailMessage email = emailsWithPDFs.get(i);
                try {
                    log.info("📧 [{}] Processing email {}/{} from: {} | Subject: '{}' at {}", 
                            SCHEDULER_NAME, i + 1, emailsWithPDFs.size(),
                            email.getFromAddress(), truncateString(email.getSubject(), 50), 
                            getCurrentTimestamp());
                    
                    // Wrap email processing in try-catch to prevent crashes
                    try {
                        processEmailWithPDFs(email);
                        log.info("✅ [{}] Email processing completed for {}/{}: {} at {}", 
                                SCHEDULER_NAME, i + 1, emailsWithPDFs.size(),
                                email.getSubject(), getCurrentTimestamp());
                    } catch (Exception processException) {
                        log.error("❌ [{}] Critical error during email processing {}/{}: {} at {}", 
                                SCHEDULER_NAME, i + 1, emailsWithPDFs.size(), 
                                email.getSubject(), getCurrentTimestamp(), processException);
                        // Continue to mark as processed to avoid reprocessing
                    }
                    
                    // 6. Mark email as processed (always try this, even if processing failed)
                    log.info("✅ [{}] Step 4: Marking email {}/{} as processed: {} at {}", 
                            SCHEDULER_NAME, i + 1, emailsWithPDFs.size(),
                            email.getMessageId(), getCurrentTimestamp());
                    try {
                        emailMonitoringService.markEmailAsProcessed(email.getMessageId());
                        log.info("✅ [{}] Successfully marked email as processed: {} at {}", 
                                SCHEDULER_NAME, email.getMessageId(), getCurrentTimestamp());
                    } catch (Exception markException) {
                        log.error("❌ [{}] EXCEPTION in markEmailAsProcessed: {} - {}", 
                                SCHEDULER_NAME, email.getMessageId(), markException.getMessage(), markException);
                        // Don't rethrow - continue processing
                    }
                    log.info("✅ [{}] Successfully processed email {}/{} from: {} at {}", 
                            SCHEDULER_NAME, i + 1, emailsWithPDFs.size(),
                            email.getFromAddress(), getCurrentTimestamp());
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("❌ [{}] CRITICAL ERROR processing email {}/{}: {} at {}", 
                            SCHEDULER_NAME, i + 1, emailsWithPDFs.size(), 
                            email.getSubject(), getCurrentTimestamp(), e);
                    failureCount++;
                    errorCount++;
                    // Continue processing other emails
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("🎉 [{}] Email polling cycle completed successfully! Processed {}/{} emails (took {}ms) at {}", 
                    SCHEDULER_NAME, successCount, emailsWithPDFs.size(), duration, getCurrentTimestamp());
            
            if (failureCount > 0) {
                log.warn("⚠️ [{}] {} emails failed to process during this cycle", SCHEDULER_NAME, failureCount);
            }
            
            lastSuccessfulRun = System.currentTimeMillis();
            isSchedulerHealthy = true;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("💥 [{}] CRITICAL ERROR during email polling cycle (took {}ms) at {}", 
                    SCHEDULER_NAME, duration, getCurrentTimestamp(), e);
            errorCount++;
            isSchedulerHealthy = false;
            // TODO: Implement alerting for critical failures
        } finally {
            // Clean up connections
            try {
                log.info("🔌 [{}] Step 5: Disconnecting from email server at {}", 
                        SCHEDULER_NAME, getCurrentTimestamp());
                emailMonitoringService.disconnect();
                log.info("✅ [{}] Disconnected from email server at {}", 
                        SCHEDULER_NAME, getCurrentTimestamp());
            } catch (Exception e) {
                log.warn("⚠️ [{}] Error during email server disconnect at {}", 
                        SCHEDULER_NAME, getCurrentTimestamp(), e);
            }
        }
        
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("🕐 [{}] === {} TRIGGER #{} COMPLETE === Total Duration: {}ms, End Time: {}", 
                SCHEDULER_NAME, SCHEDULER_NAME, totalRunCount, totalDuration, getCurrentTimestamp());
        log.info("📊 [{}] Scheduler Stats: Total Runs: {}, Errors: {}, Healthy: {}", 
                SCHEDULER_NAME, totalRunCount, errorCount, isSchedulerHealthy);
        log.info("⏱️ [{}] Next email polling cycle will start in 2 minutes (current cycle took {}ms)", 
                SCHEDULER_NAME, totalDuration);
    }
    
    /**
     * Process an individual email with PDF attachments.
     */
    private void processEmailWithPDFs(EmailMonitoringService.EmailMessage email) {
        log.debug("📎 Processing email: {} with {} PDF attachments", 
                 email.getSubject(), email.getPdfAttachments().size());
        
        int attachmentCount = 0;
        int successfulAttachments = 0;
        
        for (EmailMonitoringService.PdfAttachment pdfAttachment : email.getPdfAttachments()) {
            attachmentCount++;
            try {
                log.debug("📄 Processing attachment {}/{}: {}", 
                         attachmentCount, email.getPdfAttachments().size(), pdfAttachment.getFilename());
                
                // 4. Validate PDF attachment
                if (!isValidPdfAttachment(pdfAttachment)) {
                    log.warn("❌ Invalid PDF attachment: {}", pdfAttachment.getFilename());
                    continue;
                }
                
                // 5. Save PDF to local storage and trigger OCR processing
                log.info("📊 Processing PDF attachment: {} ({} bytes)", 
                        pdfAttachment.getFilename(), pdfAttachment.getSize());
                
                // Save PDF to local file system
                String savedFilePath = pdfStorageService.savePDF(pdfAttachment);
                log.debug("💾 Saved PDF to local storage: {}", savedFilePath);
                
                // Store PDF BLOB in database with download link
                Invoice savedInvoice = pdfDatabaseService.storePdfBlob(
                    pdfAttachment, 
                    savedFilePath, 
                    email.getSubject(), 
                    email.getFromAddress()
                );
                
                // Generate and store download URL in database
                String downloadUrl = pdfDatabaseService.generateDownloadUrl(savedInvoice.getDownloadToken());
                invoiceDataUpdateService.updateDownloadUrl(savedInvoice.getId());
                log.info("🔗 Generated and stored download link: {}", downloadUrl);
                log.debug("🗃️ Stored PDF BLOB in database with ID: {} (expires: {})", 
                    savedInvoice.getId(), savedInvoice.getDownloadExpiresAt());
                
                // Trigger OCR processing pipeline
                triggerOcrProcessing(savedInvoice, pdfAttachment, email);
                
                log.debug("✅ PDF attachment processing initiated: {}", pdfAttachment.getFilename());
                successfulAttachments++;
                
            } catch (Exception e) {
                log.error("❌ Error processing PDF attachment: {}", pdfAttachment.getFilename(), e);
                throw new RuntimeException("Failed to process PDF attachment: " + pdfAttachment.getFilename(), e);
            }
        }
        
        log.info("📋 Email processing summary: {}/{} attachments processed successfully for '{}'", 
                successfulAttachments, attachmentCount, email.getSubject());
    }
    
    /**
     * Validate PDF attachment before processing.
     */
    private boolean isValidPdfAttachment(EmailMonitoringService.PdfAttachment pdfAttachment) {
        // Check file size (max 50MB as configured)
        long maxSizeBytes = 50 * 1024 * 1024; // 50MB
        if (pdfAttachment.getSize() > maxSizeBytes) {
            log.warn("PDF attachment too large: {} ({} bytes)", 
                    pdfAttachment.getFilename(), pdfAttachment.getSize());
            return false;
        }
        
        // Check minimum file size (at least 1KB)
        if (pdfAttachment.getSize() < 1024) {
            log.warn("PDF attachment too small: {} ({} bytes)", 
                    pdfAttachment.getFilename(), pdfAttachment.getSize());
            return false;
        }
        
        // Check content type
        if (!pdfAttachment.getContentType().toLowerCase().contains("pdf")) {
            log.warn("Invalid content type for PDF: {} ({})", 
                    pdfAttachment.getFilename(), pdfAttachment.getContentType());
            return false;
        }
        
        // Check file extension
        if (!pdfAttachment.getFilename().toLowerCase().endsWith(".pdf")) {
            log.warn("Invalid file extension: {}", pdfAttachment.getFilename());
            return false;
        }
        
        return true;
    }

    /**
     * Trigger OCR processing for a saved invoice
     */
    private void triggerOcrProcessing(Invoice savedInvoice, EmailMonitoringService.PdfAttachment pdfAttachment, 
                                     EmailMonitoringService.EmailMessage email) {
        try {
            log.info("🔍 Starting OCR processing for invoice: {} (ID: {})", 
                    pdfAttachment.getFilename(), savedInvoice.getId());

            // Update invoice status to processing
            pdfDatabaseService.updateOcrStatus(savedInvoice.getId(), Invoice.OcrStatus.IN_PROGRESS, null);

            // Process PDF through OCR pipeline
            InvoiceOcrProcessingService.InvoiceProcessingResult ocrResult = 
                    ocrProcessingService.processInvoicePdf(
                            pdfAttachment.getContent(), 
                            pdfAttachment.getFilename(),
                            email.getSubject(),
                            email.getFromAddress()
                    );

            // Update database with OCR results
            updateInvoiceWithOcrResults(savedInvoice, ocrResult);

            log.info("✅ OCR processing completed for: {} | Status: {} | Confidence: {}%", 
                    pdfAttachment.getFilename(), ocrResult.getStatus(), String.format("%.1f", ocrResult.getOverallConfidence() * 100));

        } catch (Exception e) {
            log.error("❌ OCR processing failed for invoice: {} (ID: {})", 
                     pdfAttachment.getFilename(), savedInvoice.getId(), e);
            
            // Update invoice status to failed
            try {
                pdfDatabaseService.updateOcrStatus(savedInvoice.getId(), Invoice.OcrStatus.FAILED, 0.0);
                pdfDatabaseService.updateProcessingStatus(savedInvoice.getId(), Invoice.ProcessingStatus.FAILED);
            } catch (Exception dbError) {
                log.error("❌ Failed to update invoice status after OCR failure", dbError);
            }
        }
    }

    /**
     * Update invoice entity with OCR processing results
     */
    private void updateInvoiceWithOcrResults(Invoice invoice, InvoiceOcrProcessingService.InvoiceProcessingResult ocrResult) {
        try {
            // ALWAYS save raw extracted text first (regardless of pattern matching success)
            String rawText = ocrResult.getExtractedText();
            if (rawText != null && !rawText.trim().isEmpty()) {
                log.info("SAVE_RAW_TEXT [{}] Saving {} characters of extracted text for invoice {}", 
                        "EmailPollingScheduler", rawText.length(), invoice.getId());
                
                // Calculate metadata
                String ocrMethod = "TIKA"; // Default, could be enhanced to detect actual method
                Integer wordCount = rawText.trim().split("\\s+").length;
                Integer charCount = rawText.length();
                Long processingTime = 1000L; // Approximate, could be enhanced with actual timing
                
                // Save raw text and metadata
                InvoiceDataUpdateService.UpdateResult rawTextResult = invoiceDataUpdateService.updateRawTextAndMetadata(
                    invoice.getId(), rawText, ocrMethod, processingTime, wordCount, charCount);
                
                if (rawTextResult.isSuccessful()) {
                    log.info("SUCCESS [{}] Raw text saved for invoice {} ({} words, {} chars)", 
                            "EmailPollingScheduler", invoice.getId(), wordCount, charCount);
                } else {
                    log.error("FAILED [{}] Could not save raw text for invoice {}: {}", 
                            "EmailPollingScheduler", invoice.getId(), rawTextResult.getError());
                }
            } else {
                log.warn("NO_TEXT [{}] No text extracted from invoice {} to save", 
                        "EmailPollingScheduler", invoice.getId());
            }

            // Update OCR status and confidence
            Invoice.OcrStatus ocrStatus = ocrResult.isSuccessful() ? Invoice.OcrStatus.COMPLETED : Invoice.OcrStatus.FAILED;
            pdfDatabaseService.updateOcrStatus(invoice.getId(), ocrStatus, ocrResult.getOverallConfidence());

            // Update processing status based on OCR decision
            Invoice.ProcessingStatus processingStatus = determineProcessingStatus(ocrResult);
            pdfDatabaseService.updateProcessingStatus(invoice.getId(), processingStatus);

            // Extract and update invoice data if successful
            if (ocrResult.isSuccessful() && ocrResult.getExtractedData() != null) {
                updateInvoiceDataFields(invoice, ocrResult.getExtractedData());
            }

            log.debug("📊 Updated invoice {} with OCR results: OCR={}, Processing={}, Confidence={}%",
                     invoice.getId(), ocrStatus, processingStatus, String.format("%.1f", ocrResult.getOverallConfidence() * 100));

        } catch (Exception e) {
            log.error("❌ Failed to update invoice with OCR results for invoice ID: {}", invoice.getId(), e);
        }
    }

    /**
     * Determine processing status based on OCR results
     */
    private Invoice.ProcessingStatus determineProcessingStatus(InvoiceOcrProcessingService.InvoiceProcessingResult ocrResult) {
        switch (ocrResult.getStatus()) {
            case READY_FOR_AUTO_PROCESSING:
                return Invoice.ProcessingStatus.COMPLETED;
            case READY_FOR_REVIEW:
            case REQUIRES_MANUAL_REVIEW:
            case REQUIRES_MANUAL_PROCESSING:
                return Invoice.ProcessingStatus.PENDING; // Requires human review
            default:
                return Invoice.ProcessingStatus.FAILED;
        }
    }

    /**
     * Update invoice data fields with extracted data
     */
    private void updateInvoiceDataFields(Invoice invoice, InvoiceDataExtractor.ExtractedInvoiceData extractedData) {
        try {
            log.debug("📝 Updating invoice {} with extracted data: InvoiceNum={}, Amount={}, Vendor={}, Date={}",
                     invoice.getId(), 
                     extractedData.getInvoiceNumber(),
                     extractedData.getTotalAmount(),
                     extractedData.getVendorName(),
                     extractedData.getInvoiceDate());

            // Use the dedicated invoice data update service with raw text data
            // Note: Since this is called from updateInvoiceWithOcrResults, we don't have the raw text here
            // This method is used for updating extracted data only, not the raw text
            // IMPORTANT: Use COMPLETED status since this is called after successful OCR processing
            InvoiceDataUpdateService.UpdateResult result = invoiceDataUpdateService.updateInvoiceWithOcrData(
                invoice.getId(),
                extractedData,
                Invoice.OcrStatus.COMPLETED,        // Use correct status instead of cached old value
                Invoice.ProcessingStatus.COMPLETED, // Use correct status instead of cached old value
                invoice.getOcrConfidence(),
                null, // rawExtractedText - not available in this context
                null, // ocrMethod - not available in this context
                null, // processingTimeMs - not available in this context
                null, // wordCount - not available in this context
                null  // characterCount - not available in this context
            );

            if (result.isSuccessful()) {
                log.info("💾 Successfully updated invoice {} with extracted data: {} fields, {} rows affected", 
                        invoice.getId(), extractedData.getExtractedFieldCount(), result.getRowsAffected());
            } else {
                log.error("❌ Failed to update invoice {} with extracted data: {}", 
                         invoice.getId(), result.getError());
            }

        } catch (Exception e) {
            log.error("❌ Exception while updating invoice data fields for invoice ID: {}", invoice.getId(), e);
        }
    }

    /**
     * Scheduled task for email service health check.
     * Runs every 5 minutes to verify email server connectivity.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void emailServiceHealthCheck() {
        log.debug("Performing email service health check");
        
        try {
            // TODO: Implement health check logic
            // 1. Test IMAP connection
            // 2. Verify authentication
            // 3. Log connection status
            // 4. Update health metrics
            
        } catch (Exception e) {
            log.warn("Email service health check failed", e);
            // TODO: Update health status and trigger alerts if needed
        }
    }

    /**
     * Scheduled task for cleanup of processed emails.
     * Runs daily at 2 AM to clean up old processed emails.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupProcessedEmails() {
        log.info("Starting cleanup of processed emails");
        
        try {
            // TODO: Implement cleanup logic
            // 1. Find emails older than retention period
            // 2. Archive or delete processed emails
            // 3. Clean up temporary files
            // 4. Log cleanup statistics
            
        } catch (Exception e) {
            log.error("Error during email cleanup", e);
        }
    }

    // ==================== HELPER METHODS FOR DETAILED LOGGING ====================

    /**
     * Get current timestamp formatted for logging
     */
    private String getCurrentTimestamp() {
        return java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    /**
     * Get time since last successful run in human-readable format
     */
    private String getTimeSinceLastRun() {
        if (lastSuccessfulRun == 0) {
            return "never";
        }
        
        long timeDiff = System.currentTimeMillis() - lastSuccessfulRun;
        if (timeDiff < 60000) { // Less than 1 minute
            return String.format("%.1fs", timeDiff / 1000.0);
        } else if (timeDiff < 3600000) { // Less than 1 hour
            return String.format("%.1fm", timeDiff / 60000.0);
        } else {
            return String.format("%.1fh", timeDiff / 3600000.0);
        }
    }

    /**
     * Truncate string to max length for cleaner logging
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) return "null";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    /**
     * Log current scheduler health metrics
     */
    private void logSchedulerHealth() {
        log.info("🏥 [{}] Health Check: Runs={}, Errors={}, Healthy={}, Last Success={}", 
                SCHEDULER_NAME, totalRunCount, errorCount, isSchedulerHealthy, 
                lastSuccessfulRun > 0 ? getTimeSinceLastRun() + " ago" : "never");
    }

    /**
     * Add periodic heartbeat logging to verify scheduler is alive
     * This runs every 30 seconds to provide proof-of-life
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void heartbeat() {
        log.debug("HEARTBEAT [{}] SCHEDULER ALIVE at {} - (Next email check in ~{})", 
                SCHEDULER_NAME, getCurrentTimestamp(), 
                getNextEmailCheckTime());
    }

    /**
     * Calculate approximate time until next email check
     */
    private String getNextEmailCheckTime() {
        // Email polling is every 2 minutes, heartbeat is every 30 seconds
        // This is just an approximation for user awareness
        long timeToNextCheck = 120000 - (System.currentTimeMillis() % 120000);
        
        if (timeToNextCheck < 30000) {
            return String.format("%.0fs", timeToNextCheck / 1000.0);
        } else {
            return String.format("%.1fm", timeToNextCheck / 60000.0);
        }
    }
    @Scheduled(fixedRate = 5000)
    public void test() {
        System.out.println("I'm alive: " + new Date());
    }

    /**
     * Check if scheduler is healthy
     */
    public boolean isHealthy() {
        return isSchedulerHealthy;
    }

    /**
     * Get last successful run timestamp
     */
    public java.time.LocalDateTime getLastSuccessfulRun() {
        return lastSuccessfulRun > 0 ? 
            java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(lastSuccessfulRun), 
                java.time.ZoneId.systemDefault()
            ) : null;
    }
}
