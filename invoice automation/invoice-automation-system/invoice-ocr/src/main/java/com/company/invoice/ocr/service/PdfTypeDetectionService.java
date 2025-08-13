package com.company.invoice.ocr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Service for detecting PDF type and determining optimal text extraction strategy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfTypeDetectionService {

    /**
     * Analyze PDF to determine type and extraction strategy
     */
    public PdfAnalysisResult analyzePdf(byte[] pdfBytes, String filename) {
        log.debug("🔍 Analyzing PDF type for file: {} ({} bytes)", filename, pdfBytes.length);
        
        PdfAnalysisResult.PdfAnalysisResultBuilder builder = PdfAnalysisResult.builder()
                .filename(filename)
                .fileSize(pdfBytes.length);

        try {
            // Step 1: Basic PDF validation
            validatePdfStructure(pdfBytes);
            builder.isValidPdf(true);

            // Step 2: Check for text layer
            TextLayerAnalysis textAnalysis = analyzeTextLayer(pdfBytes);
            builder.hasTextLayer(textAnalysis.isHasTextLayer())
                   .textCoverage(textAnalysis.getTextCoverage())
                   .extractableText(textAnalysis.getExtractableText());

            // Step 3: Determine PDF type
            PdfType pdfType = determinePdfType(textAnalysis);
            builder.pdfType(pdfType);

            // Step 4: Recommend extraction method
            ExtractionMethod method = getRecommendedMethod(pdfType, textAnalysis);
            builder.recommendedMethod(method);

            // Step 5: Estimate processing time
            long estimatedTime = estimateProcessingTime(pdfType, pdfBytes.length);
            builder.estimatedProcessingTimeMs(estimatedTime);

            // Step 6: Calculate confidence
            double confidence = calculateDetectionConfidence(textAnalysis, pdfType);
            builder.detectionConfidence(confidence);

            log.info("📊 PDF Analysis Complete: {} | Type: {} | Method: {} | Confidence: {}%", 
                    filename, pdfType, method, String.format("%.2f", confidence * 100));

        } catch (Exception e) {
            log.error("❌ PDF analysis failed for file: {}", filename, e);
            builder.isValidPdf(false)
                   .pdfType(PdfType.CORRUPTED)
                   .recommendedMethod(ExtractionMethod.MANUAL)
                   .error(e.getMessage());
        }

        return builder.build();
    }

    /**
     * Validate basic PDF structure
     */
    private void validatePdfStructure(byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF bytes cannot be null or empty");
        }

        // Check PDF header
        if (pdfBytes.length < 4 || 
            !(pdfBytes[0] == '%' && pdfBytes[1] == 'P' && pdfBytes[2] == 'D' && pdfBytes[3] == 'F')) {
            throw new IOException("Invalid PDF header");
        }

        // Try to open PDF with PDFBox
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            if (document.getNumberOfPages() == 0) {
                throw new IOException("PDF contains no pages");
            }
        }
    }

    /**
     * Analyze text layer presence and quality
     */
    private TextLayerAnalysis analyzeTextLayer(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);
            
            // Analyze text quality
            boolean hasTextLayer = extractedText != null && !extractedText.trim().isEmpty();
            double textCoverage = calculateTextCoverage(extractedText, document.getNumberOfPages());
            
            return TextLayerAnalysis.builder()
                    .hasTextLayer(hasTextLayer)
                    .extractableText(extractedText)
                    .textCoverage(textCoverage)
                    .pageCount(document.getNumberOfPages())
                    .wordCount(hasTextLayer ? extractedText.split("\\s+").length : 0)
                    .build();
        }
    }

    /**
     * Calculate text coverage percentage
     */
    private double calculateTextCoverage(String text, int pageCount) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        // Estimate based on text density
        int wordCount = text.split("\\s+").length;
        double wordsPerPage = (double) wordCount / pageCount;
        
        // Typical invoice has 50-200 words per page
        if (wordsPerPage > 100) {
            return 1.0; // Full text coverage
        } else if (wordsPerPage > 30) {
            return 0.8; // Good text coverage
        } else if (wordsPerPage > 10) {
            return 0.5; // Partial text coverage
        } else {
            return 0.2; // Minimal text coverage
        }
    }

    /**
     * Determine PDF type based on text analysis
     */
    private PdfType determinePdfType(TextLayerAnalysis analysis) {
        if (!analysis.isHasTextLayer()) {
            return PdfType.SCANNED;
        }

        if (analysis.getTextCoverage() >= 0.8) {
            return PdfType.DIGITAL;
        }

        if (analysis.getTextCoverage() >= 0.3) {
            return PdfType.HYBRID;
        }

        return PdfType.SCANNED;
    }

    /**
     * Get recommended extraction method
     */
    private ExtractionMethod getRecommendedMethod(PdfType type, TextLayerAnalysis analysis) {
        switch (type) {
            case DIGITAL:
                return analysis.getWordCount() > 500 ? ExtractionMethod.ITEXT : ExtractionMethod.TIKA;
            case HYBRID:
                return ExtractionMethod.MULTI_METHOD;
            case SCANNED:
                return ExtractionMethod.TESSERACT;
            case CORRUPTED:
                return ExtractionMethod.MANUAL;
            default:
                return ExtractionMethod.MANUAL;
        }
    }

    /**
     * Estimate processing time in milliseconds
     */
    private long estimateProcessingTime(PdfType type, long fileSize) {
        switch (type) {
            case DIGITAL:
                return Math.max(1000, fileSize / 1024); // 1-3 seconds for digital
            case HYBRID:
                return Math.max(3000, fileSize / 512); // 3-10 seconds for hybrid
            case SCANNED:
                return Math.max(10000, fileSize / 256); // 10-30 seconds for OCR
            default:
                return 5000; // Default 5 seconds
        }
    }

    /**
     * Calculate detection confidence score
     */
    private double calculateDetectionConfidence(TextLayerAnalysis analysis, PdfType type) {
        double baseConfidence = 0.7;

        // Adjust based on text coverage
        baseConfidence += analysis.getTextCoverage() * 0.2;

        // Adjust based on type clarity
        switch (type) {
            case DIGITAL:
                if (analysis.getTextCoverage() > 0.9) baseConfidence += 0.1;
                break;
            case SCANNED:
                if (analysis.getTextCoverage() < 0.1) baseConfidence += 0.1;
                break;
            case HYBRID:
                if (analysis.getTextCoverage() > 0.3 && analysis.getTextCoverage() < 0.8) {
                    baseConfidence += 0.05;
                }
                break;
            case CORRUPTED:
                baseConfidence = 0.0;
                break;
        }

        return Math.min(1.0, Math.max(0.0, baseConfidence));
    }

    // Inner classes for structured results
    
    public enum PdfType {
        DIGITAL("Digital PDF with text layer"),
        SCANNED("Scanned/Image-based PDF"),
        HYBRID("Mixed content PDF"),
        CORRUPTED("Corrupted or invalid PDF");

        private final String description;

        PdfType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ExtractionMethod {
        TIKA("Apache Tika extraction"),
        ITEXT("iText 7 extraction"),
        TESSERACT("Tesseract OCR"),
        MULTI_METHOD("Multiple method approach"),
        MANUAL("Manual processing required");

        private final String description;

        ExtractionMethod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class PdfAnalysisResult {
        private String filename;
        private long fileSize;
        private boolean isValidPdf;
        private boolean hasTextLayer;
        private double textCoverage;
        private String extractableText;
        private PdfType pdfType;
        private ExtractionMethod recommendedMethod;
        private long estimatedProcessingTimeMs;
        private double detectionConfidence;
        private String error;

        public boolean isProcessable() {
            return isValidPdf && pdfType != PdfType.CORRUPTED;
        }

        public String getAnalysisSummary() {
            return String.format("Type: %s | Method: %s | Confidence: %.1f%% | Time: %ds",
                    pdfType, recommendedMethod, detectionConfidence * 100, 
                    estimatedProcessingTimeMs / 1000);
        }
    }

    @lombok.Data
    @lombok.Builder
    private static class TextLayerAnalysis {
        private boolean hasTextLayer;
        private String extractableText;
        private double textCoverage;
        private int pageCount;
        private int wordCount;
    }
}
