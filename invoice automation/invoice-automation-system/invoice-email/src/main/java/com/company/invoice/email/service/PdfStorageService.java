package com.company.invoice.email.service;

import com.company.invoice.email.service.EmailMonitoringService.PdfAttachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for storing PDF attachments to local file system.
 */
@Service
@Slf4j
public class PdfStorageService {

    @Value("${invoice.storage.pdf-directory:./dev-pdf-storage}")
    private String pdfStorageDirectory;

    /**
     * Save PDF attachment to local file system.
     * 
     * @param pdfAttachment The PDF attachment to save
     * @return The full path where the PDF was saved
     * @throws IOException If file cannot be saved
     */
    public String savePDF(PdfAttachment pdfAttachment) throws IOException {
        // Create storage directory if it doesn't exist
        Path storageDir = Paths.get(pdfStorageDirectory);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            log.info("📁 Created PDF storage directory: {}", storageDir.toAbsolutePath());
        }

        // Generate unique filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String originalFilename = pdfAttachment.getFilename();
        String filename = timestamp + "_" + sanitizeFilename(originalFilename);
        
        Path filePath = storageDir.resolve(filename);
        
        // Save PDF content to file
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(pdfAttachment.getContent());
        }
        
        String savedPath = filePath.toAbsolutePath().toString();
        log.info("💾 Saved PDF: {} ({} bytes) -> {}", 
                originalFilename, pdfAttachment.getContent().length, savedPath);
        
        return savedPath;
    }

    /**
     * Sanitize filename to be safe for file system.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown.pdf";
        }
        
        // Remove or replace unsafe characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Get the PDF storage directory path.
     */
    public String getStorageDirectory() {
        return Paths.get(pdfStorageDirectory).toAbsolutePath().toString();
    }

    /**
     * Check if storage directory exists and is writable.
     */
    public boolean isStorageAvailable() {
        try {
            Path storageDir = Paths.get(pdfStorageDirectory);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            return Files.isWritable(storageDir);
        } catch (Exception e) {
            log.error("❌ PDF storage directory not available: {}", pdfStorageDirectory, e);
            return false;
        }
    }
}
