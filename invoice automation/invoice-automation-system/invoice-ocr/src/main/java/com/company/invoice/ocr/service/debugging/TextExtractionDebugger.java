package com.company.invoice.ocr.service.debugging;

import com.company.invoice.ocr.service.TikaExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Debug service to help troubleshoot text extraction and pattern matching issues
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TextExtractionDebugger {

    private final TikaExtractionService tikaExtractionService;

    /**
     * Debug text extraction from a PDF file
     */
    public void debugPdfTextExtraction(String filePath) {
        try {
            Path pdfPath = Paths.get(filePath);
            if (!Files.exists(pdfPath)) {
                log.error("❌ PDF file not found: {}", filePath);
                return;
            }

            byte[] pdfBytes = Files.readAllBytes(pdfPath);
            String filename = pdfPath.getFileName().toString();

            log.info("🔍 Debugging text extraction for: {} ({} bytes)", filename, pdfBytes.length);

            // Extract text using Tika
            TikaExtractionService.TextExtractionResult result = tikaExtractionService.extractText(pdfBytes, filename);

            log.info("📊 Extraction Result:");
            log.info("  Status: {}", result.getStatus());
            log.info("  Confidence: {}", result.getConfidence());
            log.info("  Text Length: {}", result.getExtractedText() != null ? result.getExtractedText().length() : 0);
            log.info("  Processing Time: {}ms", result.getProcessingTimeMs());

            if (result.getExtractedText() != null && !result.getExtractedText().isEmpty()) {
                log.info("📝 Extracted Text (first 500 chars):");
                log.info("----------------------------------------");
                String preview = result.getExtractedText().length() > 500 ? 
                    result.getExtractedText().substring(0, 500) + "..." : result.getExtractedText();
                log.info("{}", preview);
                log.info("----------------------------------------");

                // Also log full text for pattern analysis
                log.debug("📋 Full Extracted Text:");
                log.debug("{}", result.getExtractedText());

                // Analyze text patterns
                analyzeTextPatterns(result.getExtractedText());
            } else {
                log.warn("⚠️ No text extracted from PDF");
            }

        } catch (IOException e) {
            log.error("❌ Error reading PDF file: {}", filePath, e);
        } catch (Exception e) {
            log.error("❌ Error during text extraction debug: {}", filePath, e);
        }
    }

    /**
     * Analyze text for common invoice patterns
     */
    private void analyzeTextPatterns(String text) {
        log.info("🔍 Analyzing text patterns:");

        // Check for common invoice keywords
        String[] invoiceKeywords = {"invoice", "bill", "total", "amount", "due", "date", "vendor", "company"};
        for (String keyword : invoiceKeywords) {
            boolean found = text.toLowerCase().contains(keyword.toLowerCase());
            log.info("  {} '{}': {}", found ? "✅" : "❌", keyword, found);
        }

        // Check for number patterns
        boolean hasNumbers = text.matches(".*\\d+.*");
        log.info("  {} Contains numbers: {}", hasNumbers ? "✅" : "❌", hasNumbers);

        // Check for currency symbols
        boolean hasCurrency = text.matches(".*(\\$|USD|EUR|€|£|GBP).*");
        log.info("  {} Contains currency: {}", hasCurrency ? "✅" : "❌", hasCurrency);

        // Check for date patterns
        boolean hasDate = text.matches(".*(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{4}).*");
        log.info("  {} Contains date pattern: {}", hasDate ? "✅" : "❌", hasDate);

        // Line analysis
        String[] lines = text.split("\\n");
        log.info("  📄 Total lines: {}", lines.length);
        log.info("  📏 Average line length: {}", lines.length > 0 ? text.length() / lines.length : 0);
    }
}
