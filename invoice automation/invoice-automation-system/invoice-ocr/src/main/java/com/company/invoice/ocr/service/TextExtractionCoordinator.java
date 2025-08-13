package com.company.invoice.ocr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Coordinator service that manages text extraction using multiple methods
 * and selects the best result based on confidence and quality metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TextExtractionCoordinator {

    private final PdfTypeDetectionService pdfTypeDetectionService;
    private final TikaExtractionService tikaExtractionService;
    private final TesseractOcrService tesseractOcrService;
    private final PdfBoxLayoutExtractionService pdfBoxLayoutExtractionService;

    @Value("${invoice.ocr.coordinator.min-confidence:0.7}")
    private double minimumConfidence;

    @Value("${invoice.ocr.coordinator.enable-fallback:true}")
    private boolean enableFallback;

    @Value("${invoice.ocr.coordinator.parallel-processing:true}")
    private boolean parallelProcessing;

    @Value("${invoice.ocr.coordinator.timeout-minutes:5}")
    private int timeoutMinutes;

    /**
     * Main entry point for text extraction with intelligent method selection
     */
    public ExtractionCoordinatorResult extractText(byte[] pdfBytes, String filename) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 Starting coordinated text extraction for: {} ({} bytes)", filename, pdfBytes.length);

        ExtractionCoordinatorResult.ExtractionCoordinatorResultBuilder builder = 
                ExtractionCoordinatorResult.builder()
                        .filename(filename)
                        .startTime(Instant.now());

        try {
            // Step 1: Analyze PDF type
            PdfTypeDetectionService.PdfAnalysisResult pdfAnalysis = 
                    pdfTypeDetectionService.analyzePdf(pdfBytes, filename);
            builder.pdfAnalysis(pdfAnalysis);

            if (!pdfAnalysis.isProcessable()) {
                log.warn("📋 PDF is not processable: {}", pdfAnalysis.getError());
                return builder.status(ExtractionCoordinatorStatus.FAILED)
                             .error("PDF not processable: " + pdfAnalysis.getError())
                             .build();
            }

            // Step 2: Select extraction strategy
            ExtractionStrategy strategy = selectExtractionStrategy(pdfAnalysis);
            builder.strategy(strategy);

            // Step 3: Execute extraction with selected strategy
            ExtractionResult result = executeExtractionStrategy(pdfBytes, filename, strategy, pdfAnalysis);
            builder.extractionResult(result);

            // Step 4: Validate and enhance result
            EnhancedExtractionResult enhanced = enhanceExtractionResult(result, pdfAnalysis);
            builder.enhancedResult(enhanced);

            // Step 5: Determine final status
            ExtractionCoordinatorStatus finalStatus = determineFinalStatus(enhanced);
            builder.status(finalStatus);

            long duration = System.currentTimeMillis() - startTime;
            builder.totalProcessingTimeMs(duration);

            log.info("🎉 Coordinated extraction completed: {} | Strategy: {} | Status: {} | Confidence: {:.1f}% | Time: {}ms | Method: {}",
                    filename, strategy, finalStatus, enhanced.getFinalConfidence() * 100, duration,
                    result.getPrimaryMethod() != null ? result.getPrimaryMethod() : "UNKNOWN");
            
            // DEBUG: Log detailed method usage for alignment debugging
            log.debug("🔍 Methods used: {} | Primary method: {} | Fallback: {}", 
                     result.getMethodsUsed(), result.getPrimaryMethod(), result.getFallbackMethod());

        } catch (Exception e) {
            log.error("❌ Coordinated extraction failed for: {}", filename, e);
            builder.status(ExtractionCoordinatorStatus.FAILED)
                   .error("Extraction failed: " + e.getMessage());
        }

        return builder.build();
    }

    /**
     * Select optimal extraction strategy based on PDF analysis
     */
    private ExtractionStrategy selectExtractionStrategy(PdfTypeDetectionService.PdfAnalysisResult analysis) {
        switch (analysis.getPdfType()) {
            case DIGITAL:
                // Always use MULTI_METHOD_DIGITAL for digital PDFs to get PDFBox layout preservation
                // This ensures better table structure preservation regardless of text coverage
                log.debug("📋 Digital PDF detected - using multi-method strategy for layout preservation");
                return ExtractionStrategy.MULTI_METHOD_DIGITAL;
            case HYBRID:
                return ExtractionStrategy.MULTI_METHOD_HYBRID;
            case SCANNED:
                return ExtractionStrategy.OCR_PRIMARY;
            default:
                return ExtractionStrategy.FALLBACK_CHAIN;
        }
    }

    /**
     * Execute extraction using selected strategy
     */
    private ExtractionResult executeExtractionStrategy(byte[] pdfBytes, String filename, 
                                                      ExtractionStrategy strategy,
                                                      PdfTypeDetectionService.PdfAnalysisResult analysis) {
        
        switch (strategy) {
            case TIKA_PRIMARY:
                return executeTikaPrimary(pdfBytes, filename);
                
            case OCR_PRIMARY:
                return executeOcrPrimary(pdfBytes, filename);
                
            case MULTI_METHOD_DIGITAL:
                return executeMultiMethodDigital(pdfBytes, filename);
                
            case MULTI_METHOD_HYBRID:
                return executeMultiMethodHybrid(pdfBytes, filename);
                
            case FALLBACK_CHAIN:
                return executeFallbackChain(pdfBytes, filename);
                
            default:
                throw new IllegalArgumentException("Unknown extraction strategy: " + strategy);
        }
    }

    /**
     * Execute Tika-primary extraction with alignment preservation
     */
    private ExtractionResult executeTikaPrimary(byte[] pdfBytes, String filename) {
        log.debug("📄 Executing Tika-primary strategy with alignment preservation for: {}", filename);
        
        // Use alignment-preserving Tika extraction
        TikaExtractionService.TextExtractionResult tikaResult = 
                tikaExtractionService.extractTextWithAlignment(pdfBytes, filename);

        return ExtractionResult.builder()
                .primaryMethod("Tika (Alignment Preserved)")
                .primaryResult(tikaResult.getExtractedText())
                .primaryConfidence(tikaResult.getConfidence())
                .successful(tikaResult.isSuccessful())
                .methodsUsed(List.of("Tika (Alignment Preserved)"))
                .build();
    }

    /**
     * Execute OCR-primary extraction
     */
    private ExtractionResult executeOcrPrimary(byte[] pdfBytes, String filename) {
        log.debug("🖼️ Executing OCR-primary strategy for: {}", filename);
        
        TesseractOcrService.OcrResult ocrResult = 
                tesseractOcrService.extractTextWithOcr(pdfBytes, filename);

        return ExtractionResult.builder()
                .primaryMethod("Tesseract OCR")
                .primaryResult(ocrResult.getExtractedText())
                .primaryConfidence(ocrResult.getConfidence())
                .successful(ocrResult.isSuccessful())
                .methodsUsed(List.of("Tesseract OCR"))
                .build();
    }

    /**
     * Execute multi-method extraction for digital PDFs
     */
    private ExtractionResult executeMultiMethodDigital(byte[] pdfBytes, String filename) {
        log.debug("🔄 Executing multi-method digital strategy for: {}", filename);
        
        if (parallelProcessing) {
            return executeParallelDigitalExtraction(pdfBytes, filename);
        } else {
            return executeSequentialDigitalExtraction(pdfBytes, filename);
        }
    }

    /**
     * Execute parallel extraction for digital PDFs with PDFBox layout preservation
     */
    private ExtractionResult executeParallelDigitalExtraction(byte[] pdfBytes, String filename) {
        log.debug("🚀 Starting parallel PDFBox + Tika extraction for: {}", filename);
        
        // Run PDFBox and Tika in parallel for better performance
        CompletableFuture<PdfBoxLayoutExtractionService.LayoutExtractionResult> pdfBoxFuture = 
                CompletableFuture.supplyAsync(() -> pdfBoxLayoutExtractionService.extractTextWithLayout(pdfBytes, filename));
        
        CompletableFuture<TikaExtractionService.TextExtractionResult> tikaFuture = 
                CompletableFuture.supplyAsync(() -> tikaExtractionService.extractTextWithAlignment(pdfBytes, filename));

        try {
            // Wait for both to complete
            PdfBoxLayoutExtractionService.LayoutExtractionResult pdfBoxResult = 
                    pdfBoxFuture.get(timeoutMinutes, TimeUnit.MINUTES);
            TikaExtractionService.TextExtractionResult tikaResult = 
                    tikaFuture.get(timeoutMinutes, TimeUnit.MINUTES);

            // Prefer PDFBox if it's successful and has good confidence
            if (pdfBoxResult.isSuccessful() && pdfBoxResult.getConfidence() >= minimumConfidence) {
                log.info("✅ Using PDFBox layout result (parallel)");
                return ExtractionResult.builder()
                        .primaryMethod("PDFBox Layout")
                        .primaryResult(pdfBoxResult.getExtractedText())
                        .primaryConfidence(pdfBoxResult.getConfidence())
                        .fallbackMethod("Tika (Alignment)")
                        .fallbackResult(tikaResult.getExtractedText())
                        .fallbackConfidence(tikaResult.getConfidence())
                        .successful(true)
                        .methodsUsed(List.of("PDFBox Layout", "Tika (Alignment)"))
                        .build();
            }

            // If PDFBox failed, use Tika
            if (tikaResult.isSuccessful() && tikaResult.getConfidence() >= minimumConfidence) {
                log.info("🔄 PDFBox failed, using Tika result (parallel)");
                return ExtractionResult.builder()
                        .primaryMethod("Tika (Alignment)")
                        .primaryResult(tikaResult.getExtractedText())
                        .primaryConfidence(tikaResult.getConfidence())
                        .fallbackMethod("PDFBox Layout (failed)")
                        .fallbackResult(pdfBoxResult.getExtractedText())
                        .fallbackConfidence(pdfBoxResult.getConfidence())
                        .successful(true)
                        .methodsUsed(List.of("PDFBox Layout", "Tika (Alignment)"))
                        .build();
            }

            // If both failed but we have some results, return the better one
            if (pdfBoxResult.isSuccessful() || tikaResult.isSuccessful()) {
                if (pdfBoxResult.getConfidence() >= tikaResult.getConfidence()) {
                    log.warn("⚠️ Low confidence PDFBox result used (parallel)");
                    return ExtractionResult.builder()
                            .primaryMethod("PDFBox Layout")
                            .primaryResult(pdfBoxResult.getExtractedText())
                            .primaryConfidence(pdfBoxResult.getConfidence())
                            .successful(pdfBoxResult.isSuccessful())
                            .methodsUsed(List.of("PDFBox Layout", "Tika (Alignment)"))
                            .build();
                } else {
                    log.warn("⚠️ Low confidence Tika result used (parallel)");
                    return ExtractionResult.builder()
                            .primaryMethod("Tika (Alignment)")
                            .primaryResult(tikaResult.getExtractedText())
                            .primaryConfidence(tikaResult.getConfidence())
                            .successful(tikaResult.isSuccessful())
                            .methodsUsed(List.of("PDFBox Layout", "Tika (Alignment)"))
                            .build();
                }
            }

            // Both completely failed
            log.error("❌ Both PDFBox and Tika failed in parallel extraction");
            return ExtractionResult.builder()
                    .primaryMethod("Failed")
                    .successful(false)
                    .methodsUsed(List.of("PDFBox Layout", "Tika (Alignment)"))
                    .build();

        } catch (Exception e) {
            log.error("❌ Parallel digital extraction failed for: {}", filename, e);
            return ExtractionResult.builder()
                    .primaryMethod("Failed")
                    .successful(false)
                    .build();
        }
    }

    /**
     * Execute sequential extraction for digital PDFs with layout preservation
     */
    private ExtractionResult executeSequentialDigitalExtraction(byte[] pdfBytes, String filename) {
        // Try PDFBox layout-preserving extraction first for better table structure
        log.info("🎯 Trying PDFBox layout-preserving extraction first");
        PdfBoxLayoutExtractionService.LayoutExtractionResult pdfBoxResult = 
                pdfBoxLayoutExtractionService.extractTextWithLayout(pdfBytes, filename);

        if (pdfBoxResult.isSuccessful() && pdfBoxResult.getConfidence() >= minimumConfidence) {
            return ExtractionResult.builder()
                    .primaryMethod("PDFBox Layout")
                    .primaryResult(pdfBoxResult.getExtractedText())
                    .primaryConfidence(pdfBoxResult.getConfidence())
                    .successful(true)
                    .methodsUsed(List.of("PDFBox Layout"))
                    .build();
        }

        // Try Tika as fallback
        log.info("🔄 PDFBox layout failed, trying Tika");
        TikaExtractionService.TextExtractionResult tikaResult = 
                tikaExtractionService.extractText(pdfBytes, filename);

        if (tikaResult.isSuccessful() && tikaResult.getConfidence() >= minimumConfidence) {
            return ExtractionResult.builder()
                    .primaryMethod("Tika")
                    .primaryResult(tikaResult.getExtractedText())
                    .primaryConfidence(tikaResult.getConfidence())
                    .fallbackMethod("PDFBox Layout (failed)")
                    .fallbackResult(pdfBoxResult.getExtractedText())
                    .fallbackConfidence(pdfBoxResult.getConfidence())
                    .successful(true)
                    .methodsUsed(List.of("PDFBox Layout", "Tika"))
                    .build();
        }

        // Try OCR as final fallback
        if (enableFallback) {
            log.info("🔄 Tika failed, trying OCR fallback");
            TesseractOcrService.OcrResult ocrResult = 
                    tesseractOcrService.extractTextWithOcr(pdfBytes, filename);

            return ExtractionResult.builder()
                    .primaryMethod("Tesseract OCR")
                    .primaryResult(ocrResult.getExtractedText())
                    .primaryConfidence(ocrResult.getConfidence())
                    .fallbackMethod("Multiple methods failed")
                    .fallbackResult(getBestFallbackText(pdfBoxResult, tikaResult))
                    .fallbackConfidence(Math.max(pdfBoxResult.getConfidence(), tikaResult.getConfidence()))
                    .successful(ocrResult.isSuccessful())
                    .methodsUsed(List.of("PDFBox Layout", "Tika", "Tesseract OCR"))
                    .build();
        }

        // Return best result available
        if (pdfBoxResult.getConfidence() >= tikaResult.getConfidence()) {
            return ExtractionResult.builder()
                    .primaryMethod("PDFBox Layout")
                    .primaryResult(pdfBoxResult.getExtractedText())
                    .primaryConfidence(pdfBoxResult.getConfidence())
                    .successful(pdfBoxResult.isSuccessful())
                    .methodsUsed(List.of("PDFBox Layout"))
                    .build();
        } else {
            return ExtractionResult.builder()
                    .primaryMethod("Tika")
                    .primaryResult(tikaResult.getExtractedText())
                    .primaryConfidence(tikaResult.getConfidence())
                    .successful(tikaResult.isSuccessful())
                    .methodsUsed(List.of("Tika"))
                    .build();
        }
    }

    /**
     * Get best fallback text from multiple failed attempts
     */
    private String getBestFallbackText(PdfBoxLayoutExtractionService.LayoutExtractionResult pdfBoxResult, 
                                      TikaExtractionService.TextExtractionResult tikaResult) {
        if (pdfBoxResult.getConfidence() >= tikaResult.getConfidence()) {
            return pdfBoxResult.getExtractedText();
        }
        return tikaResult.getExtractedText();
    }

    /**
     * Execute multi-method extraction for hybrid PDFs
     */
    private ExtractionResult executeMultiMethodHybrid(byte[] pdfBytes, String filename) {
        log.debug("🔄 Executing multi-method hybrid strategy for: {}", filename);
        
        // For hybrid PDFs, combine both methods
        TikaExtractionService.TextExtractionResult tikaResult = 
                tikaExtractionService.extractText(pdfBytes, filename);
        
        TesseractOcrService.OcrResult ocrResult = 
                tesseractOcrService.extractTextWithOcr(pdfBytes, filename);

        // Combine results intelligently
        String combinedText = combineExtractionResults(tikaResult.getExtractedText(), 
                                                      ocrResult.getExtractedText());
        
        double combinedConfidence = calculateCombinedConfidence(
                tikaResult.getConfidence(), ocrResult.getConfidence());

        return ExtractionResult.builder()
                .primaryMethod("Combined (Tika + OCR)")
                .primaryResult(combinedText)
                .primaryConfidence(combinedConfidence)
                .fallbackMethod("Tika")
                .fallbackResult(tikaResult.getExtractedText())
                .fallbackConfidence(tikaResult.getConfidence())
                .successful(tikaResult.isSuccessful() || ocrResult.isSuccessful())
                .methodsUsed(List.of("Tika", "Tesseract OCR", "Combined"))
                .build();
    }

    /**
     * Execute fallback chain for difficult PDFs
     */
    private ExtractionResult executeFallbackChain(byte[] pdfBytes, String filename) {
        log.debug("🔗 Executing fallback chain strategy for: {}", filename);
        
        List<String> methodsUsed = new ArrayList<>();
        
        // Try Tika first
        try {
            TikaExtractionService.TextExtractionResult tikaResult = 
                    tikaExtractionService.extractText(pdfBytes, filename);
            methodsUsed.add("Tika");
            
            if (tikaResult.isSuccessful() && tikaResult.hasMinimumContent()) {
                return ExtractionResult.builder()
                        .primaryMethod("Tika")
                        .primaryResult(tikaResult.getExtractedText())
                        .primaryConfidence(tikaResult.getConfidence())
                        .successful(true)
                        .methodsUsed(methodsUsed)
                        .build();
            }
        } catch (Exception e) {
            log.warn("⚠️ Tika extraction failed in fallback chain: {}", e.getMessage());
        }

        // Try OCR next
        try {
            TesseractOcrService.OcrResult ocrResult = 
                    tesseractOcrService.extractTextWithOcr(pdfBytes, filename);
            methodsUsed.add("Tesseract OCR");
            
            if (ocrResult.isSuccessful() && ocrResult.hasMinimumContent()) {
                return ExtractionResult.builder()
                        .primaryMethod("Tesseract OCR")
                        .primaryResult(ocrResult.getExtractedText())
                        .primaryConfidence(ocrResult.getConfidence())
                        .successful(true)
                        .methodsUsed(methodsUsed)
                        .build();
            }
        } catch (Exception e) {
            log.warn("⚠️ OCR extraction failed in fallback chain: {}", e.getMessage());
        }

        // All methods failed
        return ExtractionResult.builder()
                .primaryMethod("None")
                .successful(false)
                .methodsUsed(methodsUsed)
                .build();
    }

    /**
     * Combine text extraction results intelligently
     */
    private String combineExtractionResults(String tikaText, String ocrText) {
        if (tikaText == null || tikaText.trim().isEmpty()) {
            return ocrText != null ? ocrText : "";
        }
        
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return tikaText;
        }

        // If both have content, prefer the longer one or combine them
        if (tikaText.length() > ocrText.length() * 1.5) {
            return tikaText; // Tika result is significantly longer
        } else if (ocrText.length() > tikaText.length() * 1.5) {
            return ocrText; // OCR result is significantly longer
        } else {
            // Combine both results
            return tikaText + "\n\n--- OCR SUPPLEMENT ---\n\n" + ocrText;
        }
    }

    /**
     * Calculate combined confidence from multiple extraction methods
     */
    private double calculateCombinedConfidence(double tikaConfidence, double ocrConfidence) {
        // Weighted average with bias toward higher confidence
        double maxConfidence = Math.max(tikaConfidence, ocrConfidence);
        double avgConfidence = (tikaConfidence + ocrConfidence) / 2.0;
        
        // Give more weight to the better method
        return (maxConfidence * 0.7) + (avgConfidence * 0.3);
    }

    /**
     * Enhance extraction result with additional analysis
     */
    private EnhancedExtractionResult enhanceExtractionResult(ExtractionResult result, 
                                                            PdfTypeDetectionService.PdfAnalysisResult pdfAnalysis) {
        
        if (!result.isSuccessful() || result.getPrimaryResult() == null) {
            return EnhancedExtractionResult.builder()
                    .originalResult(result)
                    .finalText("")
                    .finalConfidence(0.0)
                    .qualityScore(0.0)
                    .recommendation(ProcessingRecommendation.MANUAL_REVIEW)
                    .build();
        }

        String text = result.getPrimaryResult();
        double confidence = result.getPrimaryConfidence();
        
        // Calculate quality score
        double qualityScore = calculateTextQuality(text);
        
        // Adjust confidence based on quality
        double adjustedConfidence = adjustConfidenceByQuality(confidence, qualityScore);
        
        // Determine processing recommendation
        ProcessingRecommendation recommendation = determineRecommendation(adjustedConfidence, qualityScore);

        // DEBUG: Log text length changes during enhancement
        log.debug("🔍 Enhancement: Original length: {} | Enhanced length: {} | Quality: {:.2f}", 
                 result.getPrimaryResult() != null ? result.getPrimaryResult().length() : 0,
                 text != null ? text.length() : 0, qualityScore);

        return EnhancedExtractionResult.builder()
                .originalResult(result)
                .finalText(text)
                .finalConfidence(adjustedConfidence)
                .qualityScore(qualityScore)
                .recommendation(recommendation)
                .wordCount(countWords(text))
                .hasInvoiceKeywords(containsInvoiceKeywords(text))
                .hasAmountPatterns(containsAmountPatterns(text))
                .hasDatePatterns(containsDatePatterns(text))
                .build();
    }

    /**
     * Calculate text quality score
     */
    private double calculateTextQuality(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        double score = 0.5; // Base score
        
        // Length factor
        if (text.length() > 200) score += 0.1;
        if (text.length() > 500) score += 0.1;
        
        // Word count factor
        int wordCount = countWords(text);
        if (wordCount > 50) score += 0.1;
        if (wordCount > 100) score += 0.1;
        
        // Structure factor
        if (text.contains("\n")) score += 0.05; // Multi-line
        if (text.split("\n").length > 10) score += 0.05; // Well-structured
        
        // Content quality
        if (containsInvoiceKeywords(text)) score += 0.1;
        if (containsAmountPatterns(text)) score += 0.1;
        if (containsDatePatterns(text)) score += 0.05;
        
        return Math.min(1.0, score);
    }

    /**
     * Adjust confidence based on text quality
     */
    private double adjustConfidenceByQuality(double originalConfidence, double qualityScore) {
        // Quality acts as a multiplier for confidence
        return Math.min(1.0, originalConfidence * (0.5 + qualityScore * 0.5));
    }

    /**
     * Determine processing recommendation
     */
    private ProcessingRecommendation determineRecommendation(double confidence, double qualityScore) {
        double combinedScore = (confidence + qualityScore) / 2.0;
        
        if (combinedScore >= 0.9) {
            return ProcessingRecommendation.AUTO_PROCESS;
        } else if (combinedScore >= 0.7) {
            return ProcessingRecommendation.REVIEW_RECOMMENDED;
        } else if (combinedScore >= 0.5) {
            return ProcessingRecommendation.MANUAL_REVIEW;
        } else {
            return ProcessingRecommendation.MANUAL_PROCESSING;
        }
    }

    /**
     * Determine final coordinator status
     */
    private ExtractionCoordinatorStatus determineFinalStatus(EnhancedExtractionResult enhanced) {
        if (!enhanced.getOriginalResult().isSuccessful()) {
            return ExtractionCoordinatorStatus.FAILED;
        }
        
        if (enhanced.getFinalConfidence() >= 0.9) {
            return ExtractionCoordinatorStatus.SUCCESS_HIGH_CONFIDENCE;
        } else if (enhanced.getFinalConfidence() >= 0.7) {
            return ExtractionCoordinatorStatus.SUCCESS_MEDIUM_CONFIDENCE;
        } else if (enhanced.getFinalConfidence() >= 0.5) {
            return ExtractionCoordinatorStatus.SUCCESS_LOW_CONFIDENCE;
        } else {
            return ExtractionCoordinatorStatus.REQUIRES_REVIEW;
        }
    }

    // Helper methods
    
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }

    private boolean containsInvoiceKeywords(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.contains("invoice") || lowerText.contains("bill") || 
               lowerText.contains("total") || lowerText.contains("amount");
    }

    private boolean containsAmountPatterns(String text) {
        return text.matches(".*\\$[0-9,]+\\.?[0-9]*.*") || 
               text.matches(".*[0-9,]+\\.[0-9]{2}.*");
    }

    private boolean containsDatePatterns(String text) {
        return text.matches(".*[0-9]{1,2}[/\\-][0-9]{1,2}[/\\-][0-9]{2,4}.*");
    }

    // Enums and Data Classes

    public enum ExtractionStrategy {
        TIKA_PRIMARY,
        OCR_PRIMARY,
        MULTI_METHOD_DIGITAL,
        MULTI_METHOD_HYBRID,
        FALLBACK_CHAIN
    }

    public enum ExtractionCoordinatorStatus {
        SUCCESS_HIGH_CONFIDENCE,
        SUCCESS_MEDIUM_CONFIDENCE,
        SUCCESS_LOW_CONFIDENCE,
        REQUIRES_REVIEW,
        FAILED
    }

    public enum ProcessingRecommendation {
        AUTO_PROCESS,
        REVIEW_RECOMMENDED,
        MANUAL_REVIEW,
        MANUAL_PROCESSING
    }

    @lombok.Data
    @lombok.Builder
    public static class ExtractionCoordinatorResult {
        private String filename;
        private Instant startTime;
        private ExtractionCoordinatorStatus status;
        private ExtractionStrategy strategy;
        private PdfTypeDetectionService.PdfAnalysisResult pdfAnalysis;
        private ExtractionResult extractionResult;
        private EnhancedExtractionResult enhancedResult;
        private long totalProcessingTimeMs;
        private String error;

        public boolean isSuccessful() {
            return status != ExtractionCoordinatorStatus.FAILED;
        }

        public String getBestText() {
            String result = enhancedResult != null ? enhancedResult.getFinalText() : 
                   (extractionResult != null ? extractionResult.getPrimaryResult() : "");
            
            // DEBUG: Log text length and preview for alignment debugging
            if (result != null && result.length() > 0) {
                String preview = result.length() > 200 ? result.substring(0, 200) + "..." : result;
                log.debug("🔍 getBestText() returning {} chars. Preview: {}", result.length(), 
                         preview.replaceAll("\\n", "\\\\n").replaceAll("\\t", "\\\\t"));
            } else {
                log.warn("🔍 getBestText() returning empty/null text!");
            }
            
            return result;
        }

        public double getBestConfidence() {
            return enhancedResult != null ? enhancedResult.getFinalConfidence() : 
                   (extractionResult != null ? extractionResult.getPrimaryConfidence() : 0.0);
        }
    }

    @lombok.Data
    @lombok.Builder
    private static class ExtractionResult {
        private String primaryMethod;
        private String primaryResult;
        private double primaryConfidence;
        private String fallbackMethod;
        private String fallbackResult;
        private double fallbackConfidence;
        private boolean successful;
        private List<String> methodsUsed;
    }

    @lombok.Data
    @lombok.Builder
    private static class EnhancedExtractionResult {
        private ExtractionResult originalResult;
        private String finalText;
        private double finalConfidence;
        private double qualityScore;
        private ProcessingRecommendation recommendation;
        private int wordCount;
        private boolean hasInvoiceKeywords;
        private boolean hasAmountPatterns;
        private boolean hasDatePatterns;
    }
}
