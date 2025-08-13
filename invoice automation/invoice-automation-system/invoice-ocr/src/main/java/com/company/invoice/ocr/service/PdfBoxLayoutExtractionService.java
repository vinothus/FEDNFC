package com.company.invoice.ocr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for extracting text from PDFs with layout preservation using PDFBox.
 * Maintains spatial positioning and table structure better than Tika.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfBoxLayoutExtractionService {

    @Value("${invoice.ocr.pdfbox.sort-by-position:true}")
    private boolean sortByPosition;

    @Value("${invoice.ocr.pdfbox.preserve-spacing:true}")
    private boolean preserveSpacing;

    /**
     * Extract text with layout preservation using PDFBox
     */
    public LayoutExtractionResult extractTextWithLayout(byte[] pdfBytes, String filename) {
        long startTime = System.currentTimeMillis();
        log.debug("📄 Starting PDFBox layout extraction for: {} ({} bytes)", filename, pdfBytes.length);

        LayoutExtractionResult.LayoutExtractionResultBuilder builder = LayoutExtractionResult.builder()
                .filename(filename)
                .extractionMethod("PDFBox Layout Preserving")
                .startTime(Instant.now());

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            
            // Method 1: Try positional text extraction
            String layoutText = extractWithPositioning(document);
            
            if (layoutText != null && !layoutText.trim().isEmpty()) {
                builder.extractedText(layoutText)
                       .confidence(calculateLayoutConfidence(layoutText))
                       .status(ExtractionStatus.SUCCESS)
                       .wordCount(countWords(layoutText))
                       .characterCount(layoutText.length())
                       .pageCount(document.getNumberOfPages());
            } else {
                // Fallback to standard PDFBox extraction
                layoutText = extractWithStandardPdfBox(document);
                builder.extractedText(layoutText)
                       .confidence(0.7) // Lower confidence for fallback
                       .status(ExtractionStatus.PARTIAL_SUCCESS)
                       .wordCount(countWords(layoutText))
                       .characterCount(layoutText.length())
                       .pageCount(document.getNumberOfPages());
            }

            long duration = System.currentTimeMillis() - startTime;
            builder.processingTimeMs(duration);

            log.info("✅ PDFBox layout extraction successful: {} | Words: {} | Confidence: {:.1f}% | Time: {}ms",
                    filename, builder.build().getWordCount(), 
                    builder.build().getConfidence() * 100, duration);
            
            // DEBUG: Log text preview for alignment verification
            String extractedText = builder.build().getExtractedText();
            if (extractedText != null && extractedText.length() > 0) {
                String preview = extractedText.length() > 300 ? extractedText.substring(0, 300) + "..." : extractedText;
                log.debug("🔍 PDFBox extracted text preview (showing tabs/spaces): {}", 
                         preview.replaceAll("\\n", "\\\\n").replaceAll("\\t", "[TAB]").replaceAll(" {2,}", "[SPACES]"));
            }

        } catch (Exception e) {
            log.error("❌ Error during PDFBox layout extraction for: {}", filename, e);
            builder.status(ExtractionStatus.FAILED)
                   .error("PDFBox extraction failed: " + e.getMessage())
                   .confidence(0.0);
        }

        return builder.build();
    }

    /**
     * Extract text with spatial positioning preserved
     */
    private String extractWithPositioning(PDDocument document) throws IOException {
        StringBuilder result = new StringBuilder();
        
        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            if (pageNum > 0) {
                result.append("\n\n=== PAGE ").append(pageNum + 1).append(" ===\n\n");
            }
            
            // Use custom stripper that preserves positioning
            PositionalTextStripper stripper = new PositionalTextStripper();
            stripper.setStartPage(pageNum + 1);
            stripper.setEndPage(pageNum + 1);
            stripper.setSortByPosition(sortByPosition);
            stripper.setShouldSeparateByBeads(false);
            
            String pageText = stripper.getText(document);
            
            if (preserveSpacing) {
                pageText = enhanceSpacing(pageText);
            }
            
            result.append(pageText);
        }
        
        return result.toString();
    }

    /**
     * Enhanced spacing to better preserve table structure
     */
    private String enhanceSpacing(String text) {
        if (text == null) return "";
        
        // Pattern for multiple spaces
        Pattern multiSpacePattern = Pattern.compile("  +");
        Matcher matcher = multiSpacePattern.matcher(text);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            int spaceCount = matcher.group().length();
            String replacement;
            if (spaceCount >= 3 && spaceCount <= 10) {
                replacement = "\t"; // Convert to tab for column alignment
            } else if (spaceCount > 10) {
                replacement = "\t\t"; // Multiple tabs for wide gaps
            } else {
                replacement = matcher.group(); // Keep original spacing
            }
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString()
                // Ensure consistent line endings
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n");
    }

    /**
     * Fallback to standard PDFBox extraction
     */
    private String extractWithStandardPdfBox(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setShouldSeparateByBeads(false);
        return stripper.getText(document);
    }

    /**
     * Calculate confidence based on layout quality
     */
    private double calculateLayoutConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        double confidence = 0.6; // Base confidence

        // Check for table-like structures
        if (text.contains("\t") || text.matches(".*\\d+\\s+\\$[\\d,]+.*")) {
            confidence += 0.2; // Likely has tables
        }

        // Check for invoice keywords
        String lowerText = text.toLowerCase();
        if (lowerText.contains("invoice") || lowerText.contains("bill")) {
            confidence += 0.1;
        }

        // Check for structured data
        if (lowerText.contains("total") && lowerText.contains("amount")) {
            confidence += 0.1;
        }

        return Math.min(1.0, confidence);
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

    /**
     * Custom PDFTextStripper for better positioning
     */
    private static class PositionalTextStripper extends PDFTextStripper {
        
        public PositionalTextStripper() throws IOException {
            super();
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            // Group text by approximate line positions
            if (textPositions.isEmpty()) {
                super.writeString(string, textPositions);
                return;
            }

            // Sort by Y position (top to bottom), then X position (left to right)
            textPositions.sort((a, b) -> {
                float yDiff = b.getYDirAdj() - a.getYDirAdj(); // Higher Y first (top to bottom)
                if (Math.abs(yDiff) < 2) { // Same line
                    return Float.compare(a.getXDirAdj(), b.getXDirAdj()); // Left to right
                }
                return Float.compare(yDiff, 0);
            });

            super.writeString(string, textPositions);
        }
    }

    // Result classes
    public enum ExtractionStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED
    }

    @lombok.Data
    @lombok.Builder
    public static class LayoutExtractionResult {
        private String filename;
        private String extractionMethod;
        private Instant startTime;
        private String extractedText;
        private double confidence;
        private ExtractionStatus status;
        private int wordCount;
        private int characterCount;
        private int pageCount;
        private Long processingTimeMs;
        private String error;

        public boolean isSuccessful() {
            return status == ExtractionStatus.SUCCESS || status == ExtractionStatus.PARTIAL_SUCCESS;
        }
    }
}
