package com.company.invoice.ocr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;

/**
 * Service for extracting text from digital PDFs using Apache Tika.
 * Optimized for PDFs with embedded text layers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TikaExtractionService {

    private final Tika tika = new Tika();

    @Value("${invoice.ocr.tika.max-string-length:100000}")
    private int maxStringLength;

    @Value("${invoice.ocr.tika.timeout-seconds:30}")
    private int timeoutSeconds;

    /**
     * Extract text from PDF using Apache Tika
     */
    public TextExtractionResult extractText(byte[] pdfBytes, String filename) {
        long startTime = System.currentTimeMillis();
        log.debug("📄 Starting Tika text extraction for: {} ({} bytes)", filename, pdfBytes.length);

        TextExtractionResult.TextExtractionResultBuilder builder = TextExtractionResult.builder()
                .filename(filename)
                .extractionMethod("Apache Tika")
                .startTime(Instant.now());

        try {
            // Configure Tika with limits
            tika.setMaxStringLength(maxStringLength);

            // Extract text with metadata
            ExtractionDetails details = extractWithMetadata(pdfBytes);
            
            // Validate and clean extracted text
            String cleanedText = cleanExtractedText(details.getText());
            
            // Calculate confidence based on extraction quality
            double confidence = calculateExtractionConfidence(cleanedText, details.getMetadata());

            builder.extractedText(cleanedText)
                   .confidence(confidence)
                   .status(ExtractionStatus.SUCCESS)
                   .wordCount(countWords(cleanedText))
                   .characterCount(cleanedText.length())
                   .metadata(details.getMetadata());

            long duration = System.currentTimeMillis() - startTime;
            builder.processingTimeMs(duration);

            log.info("✅ Tika extraction successful: {} | Words: {} | Confidence: {}% | Time: {}ms",
                    filename, builder.build().getWordCount(), String.format("%.1f", confidence * 100), duration);

        } catch (TikaException e) {
            log.error("❌ Tika extraction failed for: {} - {}", filename, e.getMessage());
            builder.status(ExtractionStatus.FAILED)
                   .error("Tika parsing failed: " + e.getMessage())
                   .confidence(0.0);
        } catch (IOException e) {
            log.error("❌ IO error during Tika extraction for: {} - {}", filename, e.getMessage());
            builder.status(ExtractionStatus.FAILED)
                   .error("IO error: " + e.getMessage())
                   .confidence(0.0);
        } catch (Exception e) {
            log.error("❌ Unexpected error during Tika extraction for: {}", filename, e);
            builder.status(ExtractionStatus.FAILED)
                   .error("Unexpected error: " + e.getMessage())
                   .confidence(0.0);
        }

        return builder.build();
    }

    /**
     * Extract text with detailed metadata using Tika parser
     */
    private ExtractionDetails extractWithMetadata(byte[] pdfBytes) throws IOException, SAXException, TikaException {
        Metadata metadata = new Metadata();
        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(maxStringLength);
        ParseContext context = new ParseContext();

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes)) {
            parser.parse(inputStream, handler, metadata, context);
        }

        return ExtractionDetails.builder()
                .text(handler.toString())
                .metadata(metadata)
                .build();
    }

    /**
     * Clean and normalize extracted text while preserving alignment
     */
    private String cleanExtractedText(String rawText) {
        if (rawText == null) {
            return "";
        }

        return rawText
                // Remove non-printable characters but keep spaces, tabs, newlines
                .replaceAll("[^\\p{Print}\\n\\t\\r ]", "")
                // Normalize line endings to \n
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                // Remove excessive newlines (more than 3)
                .replaceAll("\\n{4,}", "\n\n\n")
                // Remove trailing spaces from each line but preserve leading spaces
                .replaceAll("[ \\t]+\\n", "\n")
                // Trim only the very beginning and end
                .trim();
    }

    /**
     * Extract text with preserved formatting (minimal cleaning)
     */
    public TextExtractionResult extractTextWithAlignment(byte[] pdfBytes, String filename) {
        long startTime = System.currentTimeMillis();
        log.debug("📄 Starting Tika text extraction with alignment preservation for: {} ({} bytes)", filename, pdfBytes.length);

        TextExtractionResult.TextExtractionResultBuilder builder = TextExtractionResult.builder()
                .filename(filename)
                .extractionMethod("Apache Tika (Alignment Preserved)")
                .startTime(Instant.now());

        try {
            // Configure Tika with limits
            tika.setMaxStringLength(maxStringLength);

            // Extract text with metadata
            ExtractionDetails details = extractWithMetadata(pdfBytes);
            
            // Minimal cleaning - preserve all spacing and alignment
            String preservedText = preserveAlignment(details.getText());
            
            // Calculate confidence based on extraction quality
            double confidence = calculateExtractionConfidence(preservedText, details.getMetadata());

            builder.extractedText(preservedText)
                   .confidence(confidence)
                   .status(ExtractionStatus.SUCCESS)
                   .wordCount(countWords(preservedText))
                   .characterCount(preservedText.length())
                   .metadata(details.getMetadata());

            long duration = System.currentTimeMillis() - startTime;
            builder.processingTimeMs(duration);

            log.info("✅ Tika extraction with alignment successful: {} | Words: {} | Confidence: {}% | Time: {}ms",
                    filename, builder.build().getWordCount(), String.format("%.1f", confidence * 100), duration);

        } catch (Exception e) {
            log.error("❌ Error during Tika extraction with alignment for: {}", filename, e);
            builder.status(ExtractionStatus.FAILED)
                   .error("Extraction failed: " + e.getMessage())
                   .confidence(0.0);
        }

        return builder.build();
    }

    /**
     * Preserve text alignment with minimal cleaning
     */
    private String preserveAlignment(String rawText) {
        if (rawText == null) {
            return "";
        }

        return rawText
                // Only remove truly non-printable characters (keep all whitespace)
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                // Normalize line endings
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                // Only remove excessive empty lines (5+)
                .replaceAll("\\n{5,}", "\n\n\n\n");
    }

    /**
     * Calculate extraction confidence based on text quality and metadata
     */
    private double calculateExtractionConfidence(String text, Metadata metadata) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        double confidence = 0.5; // Base confidence

        // Text length analysis
        int length = text.length();
        if (length > 500) {
            confidence += 0.2; // Good amount of text
        } else if (length > 100) {
            confidence += 0.1; // Moderate amount of text
        }

        // Word count analysis
        int wordCount = countWords(text);
        if (wordCount > 50) {
            confidence += 0.1; // Good word count
        }

        // Check for invoice-related keywords
        confidence += calculateKeywordConfidence(text);

        // Metadata analysis
        confidence += analyzeMetadata(metadata);

        // Text structure analysis
        confidence += analyzeTextStructure(text);

        return Math.min(1.0, Math.max(0.0, confidence));
    }

    /**
     * Calculate confidence based on invoice-related keywords
     */
    private double calculateKeywordConfidence(String text) {
        String lowerText = text.toLowerCase();
        double keywordScore = 0.0;

        // Financial keywords
        String[] financialKeywords = {"invoice", "bill", "total", "amount", "payment", "due", "tax"};
        for (String keyword : financialKeywords) {
            if (lowerText.contains(keyword)) {
                keywordScore += 0.02;
            }
        }

        // Date keywords
        String[] dateKeywords = {"date", "january", "february", "march", "april", "may", "june",
                                "july", "august", "september", "october", "november", "december"};
        for (String keyword : dateKeywords) {
            if (lowerText.contains(keyword)) {
                keywordScore += 0.01;
            }
        }

        // Number patterns (amounts, dates, etc.)
        if (text.matches(".*\\$[0-9,]+\\.?[0-9]*.*")) {
            keywordScore += 0.05; // Contains currency amounts
        }
        
        if (text.matches(".*[0-9]{1,2}[/\\-][0-9]{1,2}[/\\-][0-9]{2,4}.*")) {
            keywordScore += 0.03; // Contains date patterns
        }

        return Math.min(0.2, keywordScore); // Cap at 0.2
    }

    /**
     * Analyze PDF metadata for confidence scoring
     */
    private double analyzeMetadata(Metadata metadata) {
        double metadataScore = 0.0;

        // Check if created by accounting software
        String creator = metadata.get("creator");
        if (creator != null) {
            creator = creator.toLowerCase();
            if (creator.contains("quickbooks") || creator.contains("sap") || 
                creator.contains("invoice") || creator.contains("accounting")) {
                metadataScore += 0.1;
            }
        }

        // Check if title suggests it's an invoice
        String title = metadata.get("title");
        if (title != null) {
            title = title.toLowerCase();
            if (title.contains("invoice") || title.contains("bill") || title.contains("statement")) {
                metadataScore += 0.05;
            }
        }

        return metadataScore;
    }

    /**
     * Analyze text structure for confidence scoring
     */
    private double analyzeTextStructure(String text) {
        double structureScore = 0.0;

        // Check for structured data patterns
        String[] lines = text.split("\n");
        
        // Check for table-like structures
        int tabularLines = 0;
        for (String line : lines) {
            if (line.split("\\s{3,}").length >= 3) { // Multiple columns
                tabularLines++;
            }
        }
        
        if (tabularLines > 2) {
            structureScore += 0.05; // Has table-like structure
        }

        // Check for consistent formatting
        if (lines.length > 10) {
            structureScore += 0.02; // Multi-line structured document
        }

        return structureScore;
    }

    /**
     * Count words in text
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    // Helper classes

    @lombok.Data
    @lombok.Builder
    private static class ExtractionDetails {
        private String text;
        private Metadata metadata;
    }

    public enum ExtractionStatus {
        SUCCESS,
        FAILED,
        PARTIAL,
        TIMEOUT
    }

    @lombok.Data
    @lombok.Builder
    public static class TextExtractionResult {
        private String filename;
        private String extractionMethod;
        private String extractedText;
        private ExtractionStatus status;
        private double confidence;
        private int wordCount;
        private int characterCount;
        private long processingTimeMs;
        private Instant startTime;
        private Metadata metadata;
        private String error;

        public boolean isSuccessful() {
            return status == ExtractionStatus.SUCCESS && extractedText != null && !extractedText.trim().isEmpty();
        }

        public boolean hasMinimumContent() {
            return wordCount >= 10; // At least 10 words for meaningful content
        }

        public String getSummary() {
            return String.format("Status: %s | Words: %d | Confidence: %.1f%% | Time: %dms",
                    status, wordCount, confidence * 100, processingTimeMs);
        }
    }
}
