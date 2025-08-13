package com.company.invoice.ocr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.LoadLibs;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for OCR text extraction from scanned PDFs using Tesseract.
 * Includes image preprocessing for optimal OCR accuracy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TesseractOcrService {

    @Value("${invoice.ocr.tesseract.data-path:}")
    private String tessdataPath;

    @Value("${invoice.ocr.tesseract.language:eng}")
    private String ocrLanguage;

    @Value("${invoice.ocr.tesseract.dpi:300}")
    private int renderDpi;

    @Value("${invoice.ocr.tesseract.timeout-seconds:120}")
    private int timeoutSeconds;

    @Value("${invoice.ocr.tesseract.page-seg-mode:3}")
    private int pageSegMode;

    @Value("${invoice.ocr.tesseract.ocr-engine-mode:3}")
    private int ocrEngineMode;

    private Tesseract tesseract;

    @PostConstruct
    public void initializeTesseract() {
        try {
            tesseract = new Tesseract();
            
            // Configure Tesseract data path
            if (tessdataPath.isEmpty()) {
                // Use embedded tessdata if no path specified
                File tessDataFolder = LoadLibs.extractTessResources("tessdata");
                tesseract.setDatapath(tessDataFolder.getAbsolutePath());
                log.info("📁 Using embedded Tesseract data path: {}", tessDataFolder.getAbsolutePath());
            } else {
                tesseract.setDatapath(tessdataPath);
                log.info("📁 Using configured Tesseract data path: {}", tessdataPath);
            }
            
            // Configure OCR parameters
            tesseract.setLanguage(ocrLanguage);
            tesseract.setPageSegMode(pageSegMode); // 3 = Fully automatic page segmentation
            tesseract.setOcrEngineMode(ocrEngineMode); // 3 = Default, based on what is available
            
            // Set OCR variables for better invoice processing
            tesseract.setVariable("preserve_interword_spaces", "1");
            tesseract.setVariable("user_defined_dpi", String.valueOf(renderDpi));
            tesseract.setVariable("tessedit_char_whitelist", 
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,/$%#:-()@ ");
            
            log.info("✅ Tesseract OCR initialized successfully - Language: {}, DPI: {}", 
                    ocrLanguage, renderDpi);
                    
        } catch (Exception e) {
            log.error("❌ Failed to initialize Tesseract OCR", e);
            throw new RuntimeException("Tesseract initialization failed", e);
        }
    }

    /**
     * Extract text from scanned PDF using OCR
     */
    public OcrResult extractTextWithOcr(byte[] pdfBytes, String filename) {
        long startTime = System.currentTimeMillis();
        log.debug("🖼️ Starting Tesseract OCR for: {} ({} bytes)", filename, pdfBytes.length);

        OcrResult.OcrResultBuilder builder = OcrResult.builder()
                .filename(filename)
                .extractionMethod("Tesseract OCR")
                .startTime(Instant.now())
                .renderDpi(renderDpi);

        try {
            // Convert PDF pages to images
            List<BufferedImage> pageImages = convertPdfToImages(pdfBytes);
            builder.pageCount(pageImages.size());

            // Process each page
            StringBuilder fullText = new StringBuilder();
            double totalConfidence = 0.0;
            int totalWords = 0;

            for (int i = 0; i < pageImages.size(); i++) {
                log.debug("🔍 Processing page {} of {}", i + 1, pageImages.size());
                
                PageOcrResult pageResult = processPage(pageImages.get(i), i + 1);
                fullText.append(pageResult.getText()).append("\n\n");
                totalConfidence += pageResult.getConfidence() * pageResult.getWordCount();
                totalWords += pageResult.getWordCount();
            }

            // Calculate overall confidence
            double overallConfidence = totalWords > 0 ? totalConfidence / totalWords : 0.0;
            
            // Clean and validate extracted text
            String cleanedText = cleanOcrText(fullText.toString());
            
            builder.extractedText(cleanedText)
                   .confidence(overallConfidence / 100.0) // Convert to 0-1 scale
                   .status(OcrStatus.SUCCESS)
                   .wordCount(countWords(cleanedText))
                   .characterCount(cleanedText.length());

            long duration = System.currentTimeMillis() - startTime;
            builder.processingTimeMs(duration);

            log.info("✅ OCR extraction successful: {} | Pages: {} | Words: {} | Confidence: {:.1f}% | Time: {}ms",
                    filename, pageImages.size(), builder.build().getWordCount(), overallConfidence, duration);

        } catch (Exception e) {
            log.error("❌ OCR extraction failed for: {}", filename, e);
            builder.status(OcrStatus.FAILED)
                   .error(e.getMessage())
                   .confidence(0.0);
        }

        return builder.build();
    }

    /**
     * Convert PDF pages to high-resolution images for OCR
     */
    private List<BufferedImage> convertPdfToImages(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            List<BufferedImage> images = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                try {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(
                            pageIndex, renderDpi, ImageType.RGB);
                    images.add(preprocessImage(image));
                } catch (IOException e) {
                    log.error("Failed to render page {}", pageIndex, e);
                }
            }
            return images;
        }
    }

    /**
     * Preprocess image for optimal OCR accuracy
     */
    private BufferedImage preprocessImage(BufferedImage original) {
        // Convert to grayscale
        BufferedImage processed = convertToGrayscale(original);
        
        // Enhance contrast
        processed = enhanceContrast(processed);
        
        // Apply noise reduction
        processed = reduceNoise(processed);
        
        // Sharpen image
        processed = sharpenImage(processed);
        
        return processed;
    }

    /**
     * Convert image to grayscale
     */
    private BufferedImage convertToGrayscale(BufferedImage original) {
        BufferedImage grayscale = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        
        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();
        
        return grayscale;
    }

    /**
     * Enhance image contrast
     */
    private BufferedImage enhanceContrast(BufferedImage image) {
        BufferedImage enhanced = new BufferedImage(
                image.getWidth(), image.getHeight(), image.getType());
        
        Graphics2D g2d = enhanced.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Apply contrast enhancement (simple implementation)
        float scaleFactor = 1.2f; // Increase contrast by 20%
        float offset = 0.1f;
        
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Color originalColor = new Color(image.getRGB(x, y));
                int gray = originalColor.getRed(); // Grayscale, so R=G=B
                
                // Apply contrast adjustment
                int newGray = (int) Math.max(0, Math.min(255, (gray - 128) * scaleFactor + 128 + offset));
                Color newColor = new Color(newGray, newGray, newGray);
                
                enhanced.setRGB(x, y, newColor.getRGB());
            }
        }
        
        g2d.dispose();
        return enhanced;
    }

    /**
     * Apply noise reduction
     */
    private BufferedImage reduceNoise(BufferedImage image) {
        // Simple median filter for noise reduction
        BufferedImage denoised = new BufferedImage(
                image.getWidth(), image.getHeight(), image.getType());
        
        for (int x = 1; x < image.getWidth() - 1; x++) {
            for (int y = 1; y < image.getHeight() - 1; y++) {
                int[] pixels = new int[9];
                int index = 0;
                
                // Get 3x3 neighborhood
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        Color color = new Color(image.getRGB(x + dx, y + dy));
                        pixels[index++] = color.getRed(); // Grayscale
                    }
                }
                
                // Sort pixels and take median
                java.util.Arrays.sort(pixels);
                int median = pixels[4];
                
                Color medianColor = new Color(median, median, median);
                denoised.setRGB(x, y, medianColor.getRGB());
            }
        }
        
        return denoised;
    }

    /**
     * Sharpen image for better OCR
     */
    private BufferedImage sharpenImage(BufferedImage image) {
        // Simple sharpening filter
        float[] sharpenMatrix = {
                0.0f, -1.0f, 0.0f,
                -1.0f, 5.0f, -1.0f,
                0.0f, -1.0f, 0.0f
        };
        
        BufferedImage sharpened = new BufferedImage(
                image.getWidth(), image.getHeight(), image.getType());
        
        for (int x = 1; x < image.getWidth() - 1; x++) {
            for (int y = 1; y < image.getHeight() - 1; y++) {
                float sum = 0.0f;
                int matrixIndex = 0;
                
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        Color color = new Color(image.getRGB(x + dx, y + dy));
                        sum += color.getRed() * sharpenMatrix[matrixIndex++];
                    }
                }
                
                int newValue = Math.max(0, Math.min(255, (int) sum));
                Color newColor = new Color(newValue, newValue, newValue);
                sharpened.setRGB(x, y, newColor.getRGB());
            }
        }
        
        return sharpened;
    }

    /**
     * Process individual page with OCR
     */
    private PageOcrResult processPage(BufferedImage pageImage, int pageNumber) {
        try {
            // Perform OCR with timeout
            CompletableFuture<String> textFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return tesseract.doOCR(pageImage);
                } catch (TesseractException e) {
                    throw new RuntimeException("OCR failed for page " + pageNumber, e);
                }
            });

            String text = textFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            
            // Get word-level confidence scores
            List<Word> words = tesseract.getWords(pageImage, net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel.RIL_WORD);
            double avgConfidence = words.stream()
                    .mapToDouble(Word::getConfidence)
                    .average()
                    .orElse(0.0);

            return PageOcrResult.builder()
                    .pageNumber(pageNumber)
                    .text(text)
                    .confidence(avgConfidence)
                    .wordCount(words.size())
                    .words(words)
                    .build();

        } catch (Exception e) {
            log.error("❌ OCR failed for page {}: {}", pageNumber, e.getMessage());
            return PageOcrResult.builder()
                    .pageNumber(pageNumber)
                    .text("")
                    .confidence(0.0)
                    .wordCount(0)
                    .build();
        }
    }

    /**
     * Clean OCR-extracted text while preserving alignment
     */
    private String cleanOcrText(String rawText) {
        if (rawText == null) {
            return "";
        }

        return rawText
                // Fix common OCR errors - but only isolated characters
                .replaceAll("(?i)\\bl\\b", "1") // lowercase L to 1
                .replaceAll("(?i)\\bo\\b", "0") // lowercase O to 0
                .replaceAll("(?i)rn", "m") // rn to m
                .replaceAll("(?i)cl", "d") // cl to d
                // Remove non-printable characters but preserve spaces, tabs, newlines
                .replaceAll("[^\\p{Print}\\n\\t\\r ]", "")
                // Normalize line endings to \n
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                // Remove excessive newlines (more than 3)
                .replaceAll("\\n{4,}", "\n\n\n")
                // Remove trailing spaces from each line but preserve leading spaces
                .replaceAll("[ \\t]+\\n", "\n")
                // Convert multiple spaces to tabs for better table alignment
                .replaceAll(" {4,}", "\t")
                .trim();
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

    public enum OcrStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        PARTIAL
    }

    @lombok.Data
    @lombok.Builder
    public static class OcrResult {
        private String filename;
        private String extractionMethod;
        private String extractedText;
        private OcrStatus status;
        private double confidence;
        private int wordCount;
        private int characterCount;
        private int pageCount;
        private int renderDpi;
        private long processingTimeMs;
        private Instant startTime;
        private String error;

        public boolean isSuccessful() {
            return status == OcrStatus.SUCCESS && extractedText != null && !extractedText.trim().isEmpty();
        }

        public boolean hasMinimumContent() {
            return wordCount >= 20; // OCR typically needs more words to be meaningful
        }

        public String getSummary() {
            return String.format("Status: %s | Pages: %d | Words: %d | Confidence: %.1f%% | Time: %dms",
                    status, pageCount, wordCount, confidence * 100, processingTimeMs);
        }
    }

    @lombok.Data
    @lombok.Builder
    private static class PageOcrResult {
        private int pageNumber;
        private String text;
        private double confidence;
        private int wordCount;
        private List<Word> words;
    }
}
