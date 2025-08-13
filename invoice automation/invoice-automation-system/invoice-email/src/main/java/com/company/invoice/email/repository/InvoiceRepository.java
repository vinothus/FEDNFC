package com.company.invoice.email.repository;

import com.company.invoice.email.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Invoice entity operations.
 * Provides CRUD operations and custom queries for invoice management.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Find invoice by download token
     */
    Optional<Invoice> findByDownloadToken(String downloadToken);

    /**
     * Find invoices by sender email
     */
    List<Invoice> findBySenderEmailOrderByCreatedAtDesc(String senderEmail);

    /**
     * Find invoices by processing status
     */
    List<Invoice> findByProcessingStatusOrderByCreatedAtDesc(Invoice.ProcessingStatus status);

    /**
     * Find invoices by OCR status
     */
    List<Invoice> findByOcrStatusOrderByCreatedAtDesc(Invoice.OcrStatus status);

    /**
     * Find invoices created within date range
     */
    @Query("SELECT i FROM Invoice i WHERE i.createdAt BETWEEN :startDate AND :endDate ORDER BY i.createdAt DESC")
    List<Invoice> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Find invoices by vendor name (case-insensitive)
     */
    @Query("SELECT i FROM Invoice i WHERE LOWER(i.vendorName) LIKE LOWER(CONCAT('%', :vendorName, '%')) ORDER BY i.createdAt DESC")
    List<Invoice> findByVendorNameContainingIgnoreCase(@Param("vendorName") String vendorName);

    /**
     * Find invoices with expired download links
     */
    @Query("SELECT i FROM Invoice i WHERE i.downloadExpiresAt < :currentTime")
    List<Invoice> findExpiredDownloadLinks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find invoices without PDF BLOB (for cleanup/migration)
     */
    @Query("SELECT i FROM Invoice i WHERE i.pdfBlob IS NULL")
    List<Invoice> findInvoicesWithoutPdfBlob();

    /**
     * Find invoices by filename
     */
    Optional<Invoice> findByFilename(String filename);

    /**
     * Find invoices with processing status and pagination
     */
    Page<Invoice> findByProcessingStatus(Invoice.ProcessingStatus status, Pageable pageable);

    /**
     * Count invoices by processing status
     */
    long countByProcessingStatus(Invoice.ProcessingStatus status);

    /**
     * Count invoices by OCR status
     */
    long countByOcrStatus(Invoice.OcrStatus status);

    /**
     * Find recent invoices (last N days)
     */
    @Query("SELECT i FROM Invoice i WHERE i.createdAt >= :cutoffDate ORDER BY i.createdAt DESC")
    List<Invoice> findRecentInvoices(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Calculate total storage used by PDF BLOBs
     */
    @Query("SELECT COALESCE(SUM(i.fileSize), 0) FROM Invoice i WHERE i.pdfBlob IS NOT NULL")
    Long calculateTotalBlobStorage();

    /**
     * Find invoices by checksum (to detect duplicates)
     */
    List<Invoice> findByChecksum(String checksum);

    /**
     * Check if download token exists
     */
    boolean existsByDownloadToken(String downloadToken);

    /**
     * Custom query to find invoices with download stats
     */
    @Query("SELECT i FROM Invoice i WHERE i.downloadCount >= :minDownloads ORDER BY i.downloadCount DESC")
    List<Invoice> findMostDownloadedInvoices(@Param("minDownloads") Integer minDownloads);

    // ================================
    // CUSTOM UPDATE METHODS FOR OCR DATA
    // ================================

    /**
     * Update invoice with extracted basic data
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.invoiceNumber = :invoiceNumber, " +
           "i.vendorName = :vendorName, " +
           "i.totalAmount = :totalAmount, " +
           "i.currency = :currency, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :invoiceId")
    int updateBasicInvoiceData(@Param("invoiceId") Long invoiceId,
                               @Param("invoiceNumber") String invoiceNumber,
                               @Param("vendorName") String vendorName,
                               @Param("totalAmount") BigDecimal totalAmount,
                               @Param("currency") String currency);

    /**
     * Update invoice with date information
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.invoiceDate = :invoiceDate, " +
           "i.dueDate = :dueDate, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :invoiceId")
    int updateInvoiceDates(@Param("invoiceId") Long invoiceId,
                           @Param("invoiceDate") LocalDate invoiceDate,
                           @Param("dueDate") LocalDate dueDate);

    /**
     * Update invoice with financial details
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.totalAmount = :totalAmount, " +
           "i.currency = :currency, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :invoiceId")
    int updateFinancialData(@Param("invoiceId") Long invoiceId,
                            @Param("totalAmount") BigDecimal totalAmount,
                            @Param("currency") String currency);

    /**
     * Update OCR processing status and confidence
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.ocrStatus = :ocrStatus, " +
           "i.ocrConfidence = :ocrConfidence, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :invoiceId")
    int updateOcrStatus(@Param("invoiceId") Long invoiceId,
                        @Param("ocrStatus") Invoice.OcrStatus ocrStatus,
                        @Param("ocrConfidence") BigDecimal ocrConfidence);

    /**
     * Update processing status
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.processingStatus = :processingStatus, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :invoiceId")
    int updateProcessingStatus(@Param("invoiceId") Long invoiceId,
                               @Param("processingStatus") Invoice.ProcessingStatus processingStatus);

    /**
     * Update complete invoice data from OCR extraction
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.invoiceNumber = COALESCE(:invoiceNumber, i.invoiceNumber), " +
           "i.vendorName = COALESCE(:vendorName, i.vendorName), " +
           "i.totalAmount = COALESCE(:totalAmount, i.totalAmount), " +
           "i.invoiceDate = COALESCE(:invoiceDate, i.invoiceDate), " +
           "i.dueDate = COALESCE(:dueDate, i.dueDate), " +
           "i.currency = COALESCE(:currency, i.currency), " +
           "i.processingStatus = :processingStatus, " +
           "i.ocrStatus = :ocrStatus, " +
           "i.ocrConfidence = :ocrConfidence, " +
           "i.rawExtractedText = COALESCE(:rawExtractedText, i.rawExtractedText), " +
           "i.ocrMethod = COALESCE(:ocrMethod, i.ocrMethod), " +
           "i.ocrProcessingTimeMs = COALESCE(:ocrProcessingTimeMs, i.ocrProcessingTimeMs), " +
           "i.textWordCount = COALESCE(:textWordCount, i.textWordCount), " +
           "i.textCharacterCount = COALESCE(:textCharacterCount, i.textCharacterCount), " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :invoiceId")
    int updateCompleteInvoiceData(@Param("invoiceId") Long invoiceId,
                                  @Param("invoiceNumber") String invoiceNumber,
                                  @Param("vendorName") String vendorName,
                                  @Param("totalAmount") BigDecimal totalAmount,
                                  @Param("invoiceDate") LocalDate invoiceDate,
                                  @Param("dueDate") LocalDate dueDate,
                                  @Param("currency") String currency,
                                  @Param("processingStatus") Invoice.ProcessingStatus processingStatus,
                                  @Param("ocrStatus") Invoice.OcrStatus ocrStatus,
                                  @Param("ocrConfidence") BigDecimal ocrConfidence,
                                  @Param("rawExtractedText") String rawExtractedText,
                                  @Param("ocrMethod") String ocrMethod,
                                  @Param("ocrProcessingTimeMs") Integer ocrProcessingTimeMs,
                                  @Param("textWordCount") Integer textWordCount,
                                  @Param("textCharacterCount") Integer textCharacterCount);

    /**
     * Update raw extracted text and OCR metadata
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.rawExtractedText = :rawExtractedText, " +
           "i.ocrMethod = :ocrMethod, " +
           "i.ocrProcessingTimeMs = :ocrProcessingTimeMs, " +
           "i.textWordCount = :textWordCount, " +
           "i.textCharacterCount = :textCharacterCount, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :invoiceId")
    int updateRawTextAndMetadata(@Param("invoiceId") Long invoiceId,
                                @Param("rawExtractedText") String rawExtractedText,
                                @Param("ocrMethod") String ocrMethod,
                                @Param("ocrProcessingTimeMs") Integer ocrProcessingTimeMs,
                                @Param("textWordCount") Integer textWordCount,
                                @Param("textCharacterCount") Integer textCharacterCount);

    /**
     * Batch update OCR status for multiple invoices
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.ocrStatus = :ocrStatus, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id IN :invoiceIds")
    int batchUpdateOcrStatus(@Param("invoiceIds") List<Long> invoiceIds,
                             @Param("ocrStatus") Invoice.OcrStatus ocrStatus);

    /**
     * Find invoices that need OCR processing
     */
    @Query("SELECT i FROM Invoice i WHERE " +
           "(i.ocrStatus = 'PENDING' OR i.ocrStatus = 'FAILED') AND " +
           "i.processingStatus != 'FAILED' AND " +
           "i.pdfBlob IS NOT NULL " +
           "ORDER BY i.createdAt ASC")
    List<Invoice> findInvoicesNeedingOcrProcessing();

    /**
     * Find invoices by invoice number (extracted data)
     */
    @Query("SELECT i FROM Invoice i WHERE i.invoiceNumber = :invoiceNumber ORDER BY i.createdAt DESC")
    List<Invoice> findByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

    /**
     * Find invoices by total amount range
     */
    @Query("SELECT i FROM Invoice i WHERE i.totalAmount BETWEEN :minAmount AND :maxAmount ORDER BY i.totalAmount DESC")
    List<Invoice> findByTotalAmountRange(@Param("minAmount") BigDecimal minAmount,
                                        @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Find invoices with high OCR confidence
     */
    @Query("SELECT i FROM Invoice i WHERE i.ocrConfidence >= :minConfidence ORDER BY i.ocrConfidence DESC")
    List<Invoice> findHighConfidenceInvoices(@Param("minConfidence") BigDecimal minConfidence);

    /**
     * Statistics query for OCR processing
     */
    @Query("SELECT " +
           "COUNT(i) as totalInvoices, " +
           "SUM(CASE WHEN i.ocrStatus = 'COMPLETED' THEN 1 ELSE 0 END) as completedOcr, " +
           "SUM(CASE WHEN i.ocrStatus = 'FAILED' THEN 1 ELSE 0 END) as failedOcr, " +
           "SUM(CASE WHEN i.ocrStatus = 'PENDING' THEN 1 ELSE 0 END) as pendingOcr, " +
           "AVG(i.ocrConfidence) as avgConfidence " +
           "FROM Invoice i")
    Object[] getOcrStatistics();

    /**
     * Update pattern tracking data for an invoice
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.usedPatternIds = :usedPatternIds, " +
           "i.patternMatchSummary = :patternMatchSummary, " +
           "i.extractionConfidenceDetails = :extractionConfidenceDetails, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :invoiceId")
    int updatePatternTrackingData(@Param("invoiceId") Long invoiceId,
                                  @Param("usedPatternIds") String usedPatternIds,
                                  @Param("patternMatchSummary") String patternMatchSummary,
                                  @Param("extractionConfidenceDetails") String extractionConfidenceDetails);

    /**
     * Update download URL for an invoice
     */
    @Modifying
    @Query("UPDATE Invoice i SET " +
           "i.downloadUrl = :downloadUrl, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :invoiceId")
    int updateDownloadUrl(@Param("invoiceId") Long invoiceId,
                          @Param("downloadUrl") String downloadUrl);

    // ==================== Dashboard Analytics Methods ====================

    /**
     * Get total amount sum
     */
    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.totalAmount IS NOT NULL")
    BigDecimal getTotalAmountSum();

    /**
     * Count distinct vendors
     */
    @Query("SELECT COUNT(DISTINCT i.vendorName) FROM Invoice i WHERE i.vendorName IS NOT NULL")
    long countDistinctVendors();

    /**
     * Count by received date after
     */
    long countByReceivedDateAfter(LocalDateTime date);

    /**
     * Count by received date between
     */
    long countByReceivedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get total amount by date range
     */
    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.receivedDate BETWEEN :startDate AND :endDate AND i.totalAmount IS NOT NULL")
    BigDecimal getTotalAmountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get average OCR confidence
     */
    @Query("SELECT AVG(i.ocrConfidence) FROM Invoice i WHERE i.ocrConfidence IS NOT NULL")
    BigDecimal getAverageOcrConfidence();

    /**
     * Get average processing time (based on OCR processing time in seconds)
     */
    @Query("SELECT AVG(i.ocrProcessingTimeMs) / 1000.0 FROM Invoice i WHERE i.ocrProcessingTimeMs IS NOT NULL")
    Double getAverageProcessingTime();

    // ==================== Analytics Methods ====================

    /**
     * Find earliest invoice date
     */
    @Query("SELECT MIN(i.createdAt) FROM Invoice i")
    Optional<LocalDate> findEarliestInvoiceDate();

    /**
     * Count by currency
     */
    long countByCurrency(String currency);

    /**
     * Find distinct currencies
     */
    @Query("SELECT DISTINCT i.currency FROM Invoice i WHERE i.currency IS NOT NULL ORDER BY i.currency")
    List<String> findDistinctCurrencies();

    /**
     * Find invoices by created date range and processing status
     */
    @Query("SELECT i FROM Invoice i WHERE i.createdAt BETWEEN :startDate AND :endDate AND i.processingStatus = :status ORDER BY i.createdAt DESC")
    List<Invoice> findByCreatedAtBetweenAndProcessingStatus(@Param("startDate") LocalDateTime startDate, 
                                                           @Param("endDate") LocalDateTime endDate,
                                                           @Param("status") Invoice.ProcessingStatus status);

    /**
     * Find distinct vendors
     */
    @Query("SELECT DISTINCT i.vendorName FROM Invoice i WHERE i.vendorName IS NOT NULL ORDER BY i.vendorName")
    List<String> findDistinctVendors();

    /**
     * Get processing statistics by date range
     */
    @Query("SELECT " +
           "COUNT(i) as totalCount, " +
           "SUM(CASE WHEN i.processingStatus = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount, " +
           "SUM(CASE WHEN i.processingStatus = 'PENDING' THEN 1 ELSE 0 END) as pendingCount, " +
           "SUM(CASE WHEN i.processingStatus = 'FAILED' THEN 1 ELSE 0 END) as failedCount, " +
           "AVG(CASE WHEN i.ocrProcessingTimeMs IS NOT NULL THEN i.ocrProcessingTimeMs / 1000.0 END) as avgProcessingTime " +
           "FROM Invoice i WHERE i.createdAt BETWEEN :startDate AND :endDate")
    Object[] getProcessingStatsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get amount statistics by date range
     */
    @Query("SELECT " +
           "COUNT(i) as totalCount, " +
           "SUM(CASE WHEN i.totalAmount IS NOT NULL THEN i.totalAmount ELSE 0 END) as totalAmount, " +
           "AVG(CASE WHEN i.totalAmount IS NOT NULL THEN i.totalAmount END) as avgAmount, " +
           "MIN(CASE WHEN i.totalAmount IS NOT NULL THEN i.totalAmount END) as minAmount, " +
           "MAX(CASE WHEN i.totalAmount IS NOT NULL THEN i.totalAmount END) as maxAmount " +
           "FROM Invoice i WHERE i.createdAt BETWEEN :startDate AND :endDate")
    Object[] getAmountStatsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
