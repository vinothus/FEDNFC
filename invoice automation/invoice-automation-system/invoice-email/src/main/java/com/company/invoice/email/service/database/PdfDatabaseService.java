package com.company.invoice.email.service.database;

import com.company.invoice.email.entity.Invoice;
import com.company.invoice.email.repository.InvoiceRepository;
import com.company.invoice.email.service.InvoiceDataUpdateService;
import com.company.invoice.email.service.EmailMonitoringService.PdfAttachment;
import com.company.invoice.common.service.UrlBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing PDF storage in database BLOBs with secure download links.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfDatabaseService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDataUpdateService invoiceDataUpdateService;
    private final UrlBuilderService urlBuilderService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${invoice.download.link-expiry-hours:24}")
    private int linkExpiryHours;

    @Value("${invoice.download.max-downloads:10}")
    private int maxDownloads;

    /**
     * Store PDF as BLOB in database with secure download link
     */
    @Transactional
    public Invoice storePdfBlob(PdfAttachment pdfAttachment, String filePath, String emailSubject, String senderEmail) {
        try {
            // Generate secure download token
            String downloadToken = generateSecureToken();
            
            // Calculate file checksum
            String checksum = calculateChecksum(pdfAttachment.getContent());
            
            // Check for duplicates
            List<Invoice> existingInvoices = invoiceRepository.findByChecksum(checksum);
            if (!existingInvoices.isEmpty()) {
                log.warn("🔄 Duplicate PDF detected: {} (checksum: {})", pdfAttachment.getFilename(), checksum);
                return existingInvoices.get(0); // Return existing invoice
            }

            // Create new invoice entity
            Invoice invoice = Invoice.builder()
                    .filename(extractTimestampedFilename(filePath))
                    .originalFilename(pdfAttachment.getFilename())
                    .filePath(filePath)
                    .fileSize((long) pdfAttachment.getContent().length)
                    .contentType("application/pdf")
                    .checksum(checksum)
                    .pdfBlob(pdfAttachment.getContent())
                    .downloadToken(downloadToken)
                    .downloadExpiresAt(LocalDateTime.now().plusHours(linkExpiryHours))
                    .downloadCount(0)
                    .maxDownloads(maxDownloads)
                    .emailSubject(emailSubject)
                    .senderEmail(senderEmail)
                    .receivedDate(LocalDateTime.now())
                    .processingStatus(Invoice.ProcessingStatus.PENDING)
                    .ocrStatus(Invoice.OcrStatus.PENDING)
                    .createdBy("email-polling-system")
                    .build();

            Invoice savedInvoice = invoiceRepository.save(invoice);
            
            log.info("💾 Stored PDF BLOB in database: {} ({} bytes) with download token: {}", 
                    savedInvoice.getFilename(), savedInvoice.getFileSize(), downloadToken);
            
            return savedInvoice;

        } catch (Exception e) {
            log.error("❌ Failed to store PDF BLOB in database: {}", pdfAttachment.getFilename(), e);
            throw new RuntimeException("Failed to store PDF BLOB: " + pdfAttachment.getFilename(), e);
        }
    }

    /**
     * Retrieve PDF by download token
     */
    @Transactional
    public Optional<Invoice> getPdfByDownloadToken(String downloadToken) {
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findByDownloadToken(downloadToken);
            
            if (invoiceOpt.isEmpty()) {
                log.warn("🔍 PDF not found for download token: {}", downloadToken);
                return Optional.empty();
            }
            
            Invoice invoice = invoiceOpt.get();
            
            // Check if download is still valid
            if (!invoice.isDownloadValid()) {
                log.warn("⏰ Download link expired or exceeded max downloads: {} (expires: {}, count: {}/{})", 
                        downloadToken, invoice.getDownloadExpiresAt(), invoice.getDownloadCount(), invoice.getMaxDownloads());
                return Optional.empty();
            }
            
            // Increment download count
            invoice.incrementDownloadCount();
            invoiceRepository.save(invoice);
            
            log.info("📥 PDF downloaded via token: {} (download #{}/{})", 
                    downloadToken, invoice.getDownloadCount(), invoice.getMaxDownloads());
            
            return Optional.of(invoice);

        } catch (Exception e) {
            log.error("❌ Failed to retrieve PDF by download token: {}", downloadToken, e);
            return Optional.empty();
        }
    }

    /**
     * Generate secure download URL
     */
    public String generateDownloadUrl(String downloadToken) {
        return urlBuilderService.buildDownloadUrl(downloadToken);
    }

    /**
     * Get invoice by ID
     */
    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    /**
     * Update invoice processing status
     */
    @Transactional
    public void updateProcessingStatus(Long invoiceId, Invoice.ProcessingStatus status) {
        InvoiceDataUpdateService.UpdateResult result = invoiceDataUpdateService.updateProcessingStatus(invoiceId, status);
        if (result.isSuccessful()) {
            log.debug("📊 Updated processing status for invoice {}: {}", invoiceId, status);
        } else {
            log.error("❌ Failed to update processing status for invoice {}: {}", invoiceId, result.getError());
        }
    }

    /**
     * Update OCR status and confidence
     */
    @Transactional
    public void updateOcrStatus(Long invoiceId, Invoice.OcrStatus status, Double confidence) {
        BigDecimal confidenceBd = confidence != null ? BigDecimal.valueOf(confidence) : BigDecimal.ZERO;
        InvoiceDataUpdateService.UpdateResult result = invoiceDataUpdateService.updateOcrStatus(invoiceId, status, confidenceBd);
        if (result.isSuccessful()) {
            log.debug("🔍 Updated OCR status for invoice {}: {} (confidence: {}%)", invoiceId, status, confidence);
        } else {
            log.error("❌ Failed to update OCR status for invoice {}: {}", invoiceId, result.getError());
        }
    }

    /**
     * Clean up expired download links
     */
    @Transactional
    public int cleanupExpiredDownloadLinks() {
        List<Invoice> expiredInvoices = invoiceRepository.findExpiredDownloadLinks(LocalDateTime.now());
        
        for (Invoice invoice : expiredInvoices) {
            // Option 1: Generate new token
            invoice.setDownloadToken(generateSecureToken());
            invoice.setDownloadExpiresAt(LocalDateTime.now().plusHours(linkExpiryHours));
            invoice.setDownloadCount(0);
            
            // Option 2: Or delete the BLOB to save space (uncomment if preferred)
            // invoice.setPdfBlob(null);
        }
        
        if (!expiredInvoices.isEmpty()) {
            invoiceRepository.saveAll(expiredInvoices);
            log.info("🧹 Cleaned up {} expired download links", expiredInvoices.size());
        }
        
        return expiredInvoices.size();
    }

    /**
     * Get storage statistics
     */
    public StorageStats getStorageStats() {
        long totalCount = invoiceRepository.count();
        long totalBlobStorage = invoiceRepository.calculateTotalBlobStorage();
        long pendingCount = invoiceRepository.countByProcessingStatus(Invoice.ProcessingStatus.PENDING);
        long completedCount = invoiceRepository.countByProcessingStatus(Invoice.ProcessingStatus.COMPLETED);
        
        return new StorageStats(totalCount, totalBlobStorage, pendingCount, completedCount);
    }

    /**
     * Generate cryptographically secure token
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32]; // 256-bit token
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Calculate SHA-256 checksum for duplicate detection
     */
    private String calculateChecksum(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to calculate checksum", e);
            return null;
        }
    }

    /**
     * Extract filename from full file path
     */
    private String extractTimestampedFilename(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    /**
     * Storage statistics data class
     */
    public record StorageStats(
            long totalInvoices,
            long totalBlobStorageBytes,
            long pendingInvoices,
            long completedInvoices
    ) {
        public double getTotalBlobStorageMB() {
            return totalBlobStorageBytes / (1024.0 * 1024.0);
        }
    }
}
