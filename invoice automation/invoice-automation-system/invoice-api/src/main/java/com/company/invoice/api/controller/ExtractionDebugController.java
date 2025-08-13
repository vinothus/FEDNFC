package com.company.invoice.api.controller;

import com.company.invoice.ocr.service.PdfBoxLayoutExtractionService;
import com.company.invoice.ocr.service.TikaExtractionService;
import com.company.invoice.ocr.service.TextExtractionCoordinator;
import com.company.invoice.email.repository.InvoiceRepository;
import com.company.invoice.email.entity.Invoice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Debug controller to test different text extraction methods
 */
@RestController
@RequestMapping("/api/debug/extraction")
@Tag(name = "Extraction Debug", description = "Debug text extraction methods")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ExtractionDebugController {

    private final InvoiceRepository invoiceRepository;
    private final TextExtractionCoordinator textExtractionCoordinator;
    private final TikaExtractionService tikaExtractionService;
    private final PdfBoxLayoutExtractionService pdfBoxLayoutExtractionService;

    /**
     * Test all extraction methods on a specific invoice
     */
    @PostMapping("/test-methods/{invoiceId}")
    @Operation(summary = "Test all extraction methods", 
               description = "Run all extraction methods on an invoice and compare results")
    public ResponseEntity<?> testAllMethods(@PathVariable Long invoiceId) {
        
        log.info("🧪 Testing all extraction methods for invoice: {}", invoiceId);
        
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            
            if (invoiceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Invoice invoice = invoiceOpt.get();
            byte[] pdfBytes = invoice.getPdfBlob();
            String filename = invoice.getOriginalFilename();
            
            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.badRequest().body("No PDF data available for this invoice");
            }
            
            Map<String, Object> results = new HashMap<>();
            
            // Test 1: Current coordinated extraction
            log.info("🔬 Testing coordinated extraction...");
            TextExtractionCoordinator.ExtractionCoordinatorResult coordinatorResult = 
                    textExtractionCoordinator.extractText(pdfBytes, filename);
            results.put("coordinator", Map.of(
                "method", coordinatorResult.getStrategy(),
                "confidence", coordinatorResult.getBestConfidence(),
                "text", coordinatorResult.getBestText(),
                "textLength", coordinatorResult.getBestText() != null ? coordinatorResult.getBestText().length() : 0,
                "successful", coordinatorResult.isSuccessful()
            ));
            
            // Test 2: PDFBox Layout (direct)
            log.info("🔬 Testing PDFBox layout extraction...");
            PdfBoxLayoutExtractionService.LayoutExtractionResult pdfBoxResult = 
                    pdfBoxLayoutExtractionService.extractTextWithLayout(pdfBytes, filename);
            results.put("pdfbox", Map.of(
                "method", "PDFBox Layout",
                "confidence", pdfBoxResult.getConfidence(),
                "text", pdfBoxResult.getExtractedText() != null ? pdfBoxResult.getExtractedText() : "",
                "textLength", pdfBoxResult.getExtractedText() != null ? pdfBoxResult.getExtractedText().length() : 0,
                "successful", pdfBoxResult.isSuccessful()
            ));
            
            // Test 3: Tika (standard)
            log.info("🔬 Testing Tika standard extraction...");
            TikaExtractionService.TextExtractionResult tikaStandardResult = 
                    tikaExtractionService.extractText(pdfBytes, filename);
            results.put("tika_standard", Map.of(
                "method", "Tika Standard",
                "confidence", tikaStandardResult.getConfidence(),
                "text", tikaStandardResult.getExtractedText() != null ? tikaStandardResult.getExtractedText() : "",
                "textLength", tikaStandardResult.getExtractedText() != null ? tikaStandardResult.getExtractedText().length() : 0,
                "successful", tikaStandardResult.isSuccessful()
            ));
            
            // Test 4: Tika (alignment preserved)
            log.info("🔬 Testing Tika alignment-preserved extraction...");
            TikaExtractionService.TextExtractionResult tikaAlignmentResult = 
                    tikaExtractionService.extractTextWithAlignment(pdfBytes, filename);
            results.put("tika_alignment", Map.of(
                "method", "Tika Alignment Preserved",
                "confidence", tikaAlignmentResult.getConfidence(),
                "text", tikaAlignmentResult.getExtractedText() != null ? tikaAlignmentResult.getExtractedText() : "",
                "textLength", tikaAlignmentResult.getExtractedText() != null ? tikaAlignmentResult.getExtractedText().length() : 0,
                "successful", tikaAlignmentResult.isSuccessful()
            ));
            
            // Summary
            results.put("summary", Map.of(
                "invoiceId", invoiceId,
                "filename", filename,
                "pdfSize", pdfBytes.length,
                "testTimestamp", System.currentTimeMillis()
            ));
            
            log.info("✅ All extraction methods tested for invoice: {}", invoiceId);
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            log.error("❌ Error during extraction method testing for invoice {}", invoiceId, e);
            return ResponseEntity.internalServerError().body("Test failed: " + e.getMessage());
        }
    }
}
