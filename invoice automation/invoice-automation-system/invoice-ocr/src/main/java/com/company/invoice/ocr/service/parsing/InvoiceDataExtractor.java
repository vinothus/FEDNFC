package com.company.invoice.ocr.service.parsing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main service for extracting structured invoice data from OCR text.
 * Orchestrates pattern matching and data validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceDataExtractor {

    private final InvoicePatternLibrary patternLibrary; // Legacy - kept for backward compatibility
    private final DatabasePatternLibrary databasePatternLibrary; // New database-driven patterns
    private final ExtractionValidator validator;
    private final ConfidenceCalculator confidenceCalculator;

    private final ExecutorService extractionExecutor = Executors.newFixedThreadPool(4);

    /**
     * Extract all invoice data from text
     */
    public InvoiceExtractionResult extractInvoiceData(String text, String filename, String emailSubject, String senderEmail) {
        long startTime = System.currentTimeMillis();
        log.info("📋 Starting invoice data extraction for: {}", filename);

        InvoiceExtractionResult.InvoiceExtractionResultBuilder builder = InvoiceExtractionResult.builder()
                .filename(filename)
                .emailSubject(emailSubject)
                .senderEmail(senderEmail)
                .extractionStartTime(Instant.now())
                .inputText(text)
                .inputTextLength(text != null ? text.length() : 0);

        try {
            // Extract all fields in parallel for better performance
            ExtractedInvoiceData extractedData = extractAllFieldsParallel(text);
            builder.extractedData(extractedData);

            // Enhance extraction with email context
            ExtractedInvoiceData enhancedData = enhanceWithEmailContext(extractedData, emailSubject, senderEmail);
            builder.enhancedData(enhancedData);

            // Validate extracted data
            ValidationResult validation = validator.validate(enhancedData);
            builder.validationResult(validation);

            // Calculate overall confidence
            double overallConfidence = confidenceCalculator.calculateOverallConfidence(enhancedData, validation);
            builder.overallConfidence(overallConfidence);

            // Determine processing recommendation
            ProcessingRecommendation recommendation = determineProcessingRecommendation(overallConfidence, validation);
            builder.recommendation(recommendation);

            // Set final status
            ExtractionStatus status = determineExtractionStatus(enhancedData, validation, overallConfidence);
            builder.status(status);

            long duration = System.currentTimeMillis() - startTime;
            builder.processingTimeMs(duration);

            log.info("✅ Invoice data extraction completed: {} | Status: {} | Confidence: {}% | Fields: {} | Time: {}ms",
                    filename, status, String.format("%.1f", overallConfidence * 100), countExtractedFields(enhancedData), duration);

        } catch (Exception e) {
            log.error("❌ Invoice data extraction failed for: {}", filename, e);
            builder.status(ExtractionStatus.FAILED)
                   .error(e.getMessage())
                   .overallConfidence(0.0);
        }

        return builder.build();
    }

    /**
     * Extract all fields in parallel for better performance using database patterns
     */
    private ExtractedInvoiceData extractAllFieldsParallel(String text) {
        List<CompletableFuture<DatabasePatternLibrary.ExtractionResult>> futures = new ArrayList<>();

        // Submit all extraction tasks using database patterns
        futures.add(CompletableFuture.supplyAsync(() -> databasePatternLibrary.extractInvoiceNumber(text), extractionExecutor));
        futures.add(CompletableFuture.supplyAsync(() -> databasePatternLibrary.extractTotalAmount(text), extractionExecutor));
        futures.add(CompletableFuture.supplyAsync(() -> databasePatternLibrary.extractSubtotalAmount(text), extractionExecutor));
        futures.add(CompletableFuture.supplyAsync(() -> databasePatternLibrary.extractTaxAmount(text), extractionExecutor));
        futures.add(CompletableFuture.supplyAsync(() -> databasePatternLibrary.extractInvoiceDate(text), extractionExecutor));
        futures.add(CompletableFuture.supplyAsync(() -> databasePatternLibrary.extractDueDate(text), extractionExecutor));
        futures.add(CompletableFuture.supplyAsync(() -> databasePatternLibrary.extractVendorName(text), extractionExecutor));
        futures.add(CompletableFuture.supplyAsync(() -> databasePatternLibrary.extractEmail(text), extractionExecutor)); // Email from vendor info
        futures.add(CompletableFuture.supplyAsync(() -> databasePatternLibrary.extractCustomer(text), extractionExecutor)); // Customer info

        // Wait for all extractions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect results
        ExtractedInvoiceData.ExtractedInvoiceDataBuilder dataBuilder = ExtractedInvoiceData.builder();
        List<FieldExtraction> fieldExtractions = new ArrayList<>();

        try {
            // Invoice Number
            DatabasePatternLibrary.ExtractionResult invoiceNumberResult = futures.get(0).get();
            if (invoiceNumberResult.value() != null && !invoiceNumberResult.value().trim().isEmpty()) {
                dataBuilder.invoiceNumber(invoiceNumberResult.value());
                fieldExtractions.add(FieldExtraction.from(invoiceNumberResult, "Invoice Number"));
            }

            // Total Amount
            DatabasePatternLibrary.ExtractionResult totalAmountResult = futures.get(1).get();
            if (totalAmountResult.value() != null && !totalAmountResult.value().trim().isEmpty()) {
                try {
                    // Remove commas before parsing BigDecimal
                    String cleanAmount = totalAmountResult.value().replaceAll(",", "");
                    dataBuilder.totalAmount(new BigDecimal(cleanAmount));
                    fieldExtractions.add(FieldExtraction.from(totalAmountResult, "Total Amount"));
                    log.debug("✅ Parsed total amount: {} -> {}", totalAmountResult.value(), cleanAmount);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ Invalid total amount format: {}", totalAmountResult.value());
                }
            }

            // Subtotal Amount
            DatabasePatternLibrary.ExtractionResult subtotalResult = futures.get(2).get();
            if (subtotalResult.value() != null && !subtotalResult.value().trim().isEmpty()) {
                try {
                    // Remove commas before parsing BigDecimal
                    String cleanSubtotal = subtotalResult.value().replaceAll(",", "");
                    dataBuilder.subtotalAmount(new BigDecimal(cleanSubtotal));
                    fieldExtractions.add(FieldExtraction.from(subtotalResult, "Subtotal Amount"));
                    log.debug("✅ Parsed subtotal amount: {} -> {}", subtotalResult.value(), cleanSubtotal);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ Invalid subtotal amount format: {}", subtotalResult.value());
                }
            }

            // Tax Amount
            DatabasePatternLibrary.ExtractionResult taxResult = futures.get(3).get();
            if (taxResult.value() != null && !taxResult.value().trim().isEmpty()) {
                try {
                    // Remove commas before parsing BigDecimal
                    String cleanTax = taxResult.value().replaceAll(",", "");
                    dataBuilder.taxAmount(new BigDecimal(cleanTax));
                    fieldExtractions.add(FieldExtraction.from(taxResult, "Tax Amount"));
                    log.debug("✅ Parsed tax amount: {} -> {}", taxResult.value(), cleanTax);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ Invalid tax amount format: {}", taxResult.value());
                }
            }

            // Invoice Date
            DatabasePatternLibrary.ExtractionResult invoiceDateResult = futures.get(4).get();
            if (invoiceDateResult.value() != null && !invoiceDateResult.value().trim().isEmpty()) {
                try {
                    dataBuilder.invoiceDate(LocalDate.parse(invoiceDateResult.value()));
                    fieldExtractions.add(FieldExtraction.from(invoiceDateResult, "Invoice Date"));
                } catch (Exception e) {
                    log.warn("⚠️ Invalid invoice date format: {}", invoiceDateResult.value());
                }
            }

            // Due Date
            DatabasePatternLibrary.ExtractionResult dueDateResult = futures.get(5).get();
            if (dueDateResult.value() != null && !dueDateResult.value().trim().isEmpty()) {
                try {
                    dataBuilder.dueDate(LocalDate.parse(dueDateResult.value()));
                    fieldExtractions.add(FieldExtraction.from(dueDateResult, "Due Date"));
                } catch (Exception e) {
                    log.warn("⚠️ Invalid due date format: {}", dueDateResult.value());
                }
            }

            // Vendor Name
            DatabasePatternLibrary.ExtractionResult vendorNameResult = futures.get(6).get();
            if (vendorNameResult.value() != null && !vendorNameResult.value().trim().isEmpty()) {
                dataBuilder.vendorName(vendorNameResult.value());
                fieldExtractions.add(FieldExtraction.from(vendorNameResult, "Vendor Name"));
            }

            // Vendor Email
            DatabasePatternLibrary.ExtractionResult vendorEmailResult = futures.get(7).get();
            if (vendorEmailResult.value() != null && !vendorEmailResult.value().trim().isEmpty()) {
                dataBuilder.vendorEmail(vendorEmailResult.value());
                fieldExtractions.add(FieldExtraction.from(vendorEmailResult, "Vendor Email"));
            }

            // Customer Info
            DatabasePatternLibrary.ExtractionResult customerResult = futures.get(8).get();
            if (customerResult.value() != null && !customerResult.value().trim().isEmpty()) {
                // For now, store customer info as a note - can be expanded later
                fieldExtractions.add(FieldExtraction.from(customerResult, "Customer Info"));
            }

        } catch (Exception e) {
            log.error("Error collecting extraction results", e);
        }

        return dataBuilder.fieldExtractions(fieldExtractions)
                         .extractionMethod("parallel_pattern_matching")
                         .extractedAt(Instant.now())
                         .build();
    }

    /**
     * Enhance extraction with email context
     */
    private ExtractedInvoiceData enhanceWithEmailContext(ExtractedInvoiceData data, String emailSubject, String senderEmail) {
        ExtractedInvoiceData.ExtractedInvoiceDataBuilder enhancedBuilder = data.toBuilder();

        // Enhance vendor name with email domain if missing or low confidence
        if (data.getVendorName() == null || getFieldConfidence(data, "vendor_name") < 0.7) {
            String enhancedVendorName = enhanceVendorNameFromEmail(data.getVendorName(), senderEmail, emailSubject);
            if (enhancedVendorName != null) {
                enhancedBuilder.vendorName(enhancedVendorName);
                
                // Add enhancement record
                List<FieldExtraction> enhanced = new ArrayList<>(data.getFieldExtractions());
                enhanced.add(FieldExtraction.builder()
                        .fieldName("vendor_name")
                        .value(enhancedVendorName)
                        .confidence(0.8)
                        .extractionMethod("email_enhancement")
                        .sourceText(senderEmail)
                        .build());
                enhancedBuilder.fieldExtractions(enhanced);
            }
        }

        // Enhance invoice number from email subject if missing
        if (data.getInvoiceNumber() == null) {
            String invoiceFromSubject = extractInvoiceNumberFromSubject(emailSubject);
            if (invoiceFromSubject != null) {
                enhancedBuilder.invoiceNumber(invoiceFromSubject);
                
                List<FieldExtraction> enhanced = new ArrayList<>(data.getFieldExtractions());
                enhanced.add(FieldExtraction.builder()
                        .fieldName("invoice_number")
                        .value(invoiceFromSubject)
                        .confidence(0.7)
                        .extractionMethod("email_subject")
                        .sourceText(emailSubject)
                        .build());
                enhancedBuilder.fieldExtractions(enhanced);
            }
        }

        // Set currency default
        if (data.getCurrency() == null) {
            enhancedBuilder.currency("USD");
        }

        return enhancedBuilder.build();
    }

    /**
     * Enhance vendor name from email information
     */
    private String enhanceVendorNameFromEmail(String existingVendorName, String senderEmail, String emailSubject) {
        if (senderEmail == null || senderEmail.isEmpty()) {
            return existingVendorName;
        }

        // Extract domain name from email
        String domain = senderEmail.substring(senderEmail.indexOf('@') + 1);
        if (domain.contains(".")) {
            String companyName = domain.substring(0, domain.indexOf('.'));
            
            // Clean up common email providers
            if (companyName.equals("gmail") || companyName.equals("yahoo") || 
                companyName.equals("hotmail") || companyName.equals("outlook")) {
                return existingVendorName; // Don't use generic email providers
            }
            
            // Convert domain to company name format
            companyName = companyName.substring(0, 1).toUpperCase() + companyName.substring(1);
            
            return existingVendorName != null ? existingVendorName : companyName;
        }

        return existingVendorName;
    }

    /**
     * Extract invoice number from email subject
     */
    private String extractInvoiceNumberFromSubject(String emailSubject) {
        if (emailSubject == null || emailSubject.isEmpty()) {
            return null;
        }

        // Look for patterns like "Invoice #123456" or "INV-2024-001"
        InvoicePatternLibrary.ExtractionResult result = patternLibrary.extractInvoiceNumber(emailSubject);
        return result.isFound() ? result.getValue() : null;
    }

    /**
     * Get confidence for a specific field
     */
    private double getFieldConfidence(ExtractedInvoiceData data, String fieldName) {
        return data.getFieldExtractions().stream()
                .filter(fe -> fe.getFieldName().equals(fieldName))
                .mapToDouble(FieldExtraction::getConfidence)
                .max()
                .orElse(0.0);
    }

    /**
     * Count successfully extracted fields
     */
    private int countExtractedFields(ExtractedInvoiceData data) {
        int count = 0;
        if (data.getInvoiceNumber() != null) count++;
        if (data.getTotalAmount() != null) count++;
        if (data.getSubtotalAmount() != null) count++;
        if (data.getTaxAmount() != null) count++;
        if (data.getInvoiceDate() != null) count++;
        if (data.getDueDate() != null) count++;
        if (data.getVendorName() != null) count++;
        if (data.getVendorAddress() != null) count++;
        if (data.getVendorEmail() != null) count++;
        return count;
    }

    /**
     * Determine processing recommendation
     */
    private ProcessingRecommendation determineProcessingRecommendation(double confidence, ValidationResult validation) {
        if (confidence >= 0.9 && validation.isValid()) {
            return ProcessingRecommendation.AUTO_APPROVE;
        } else if (confidence >= 0.7 && (validation.isValid() || validation.hasOnlyWarnings())) {
            return ProcessingRecommendation.REVIEW_RECOMMENDED;
        } else if (confidence >= 0.5) {
            return ProcessingRecommendation.MANUAL_REVIEW;
        } else {
            return ProcessingRecommendation.MANUAL_PROCESSING;
        }
    }

    /**
     * Determine extraction status
     */
    private ExtractionStatus determineExtractionStatus(ExtractedInvoiceData data, ValidationResult validation, double confidence) {
        int extractedCount = countExtractedFields(data);
        
        if (extractedCount == 0) {
            return ExtractionStatus.NO_DATA_EXTRACTED;
        } else if (extractedCount >= 6 && confidence >= 0.8) {
            return ExtractionStatus.EXTRACTION_COMPLETE;
        } else if (extractedCount >= 3 && confidence >= 0.6) {
            return ExtractionStatus.PARTIAL_EXTRACTION;
        } else {
            return ExtractionStatus.LOW_CONFIDENCE;
        }
    }

    // Enums and Data Classes

    public enum ExtractionStatus {
        EXTRACTION_COMPLETE,
        PARTIAL_EXTRACTION,
        LOW_CONFIDENCE,
        NO_DATA_EXTRACTED,
        FAILED
    }

    public enum ProcessingRecommendation {
        AUTO_APPROVE,
        REVIEW_RECOMMENDED,
        MANUAL_REVIEW,
        MANUAL_PROCESSING
    }

    @lombok.Data
    @lombok.Builder(toBuilder = true)
    public static class ExtractedInvoiceData {
        // Basic Invoice Information
        private String invoiceNumber;
        private String vendorInvoiceNumber;
        private String purchaseOrderNumber;
        
        // Financial Information
        private BigDecimal totalAmount;
        private BigDecimal subtotalAmount;
        private BigDecimal taxAmount;
        private BigDecimal discountAmount;
        private String currency;
        
        // Date Information
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private LocalDate servicePeriodStart;
        private LocalDate servicePeriodEnd;
        
        // Vendor Information
        private String vendorName;
        private String vendorAddress;
        private String vendorTaxId;
        private String vendorEmail;
        private String vendorPhone;
        
        // Processing Metadata
        private String extractionMethod;
        private List<FieldExtraction> fieldExtractions;
        private Instant extractedAt;

        public boolean hasRequiredFields() {
            return invoiceNumber != null && totalAmount != null && vendorName != null;
        }

        public boolean hasFinancialData() {
            return totalAmount != null;
        }

        public int getExtractedFieldCount() {
            int count = 0;
            if (invoiceNumber != null) count++;
            if (totalAmount != null) count++;
            if (subtotalAmount != null) count++;
            if (taxAmount != null) count++;
            if (invoiceDate != null) count++;
            if (dueDate != null) count++;
            if (vendorName != null) count++;
            if (vendorAddress != null) count++;
            if (vendorEmail != null) count++;
            return count;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class FieldExtraction {
        private String fieldName;
        private String value;
        private double confidence;
        private String extractionMethod;
        private String sourceText;
        private int lineNumber;
        private String pattern;

        public static FieldExtraction from(InvoicePatternLibrary.ExtractionResult result) {
            return FieldExtraction.builder()
                    .fieldName(result.getFieldName())
                    .value(result.getValue())
                    .confidence(result.getConfidence())
                    .extractionMethod(result.getExtractionMethod())
                    .sourceText(result.getSourceText())
                    .lineNumber(result.getLineNumber())
                    .pattern(result.getPattern())
                    .build();
        }

        // New method for database pattern results
        public static FieldExtraction from(DatabasePatternLibrary.ExtractionResult result, String fieldName) {
            return FieldExtraction.builder()
                    .fieldName(fieldName)
                    .value(result.value())
                    .confidence(result.confidence())
                    .extractionMethod("DATABASE_PATTERN")
                    .pattern(result.patternUsed())
                    .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class InvoiceExtractionResult {
        private String filename;
        private String emailSubject;
        private String senderEmail;
        private Instant extractionStartTime;
        private String inputText;
        private int inputTextLength;
        private ExtractedInvoiceData extractedData;
        private ExtractedInvoiceData enhancedData;
        private ValidationResult validationResult;
        private double overallConfidence;
        private ProcessingRecommendation recommendation;
        private ExtractionStatus status;
        private long processingTimeMs;
        private String error;

        public boolean isSuccessful() {
            return status != ExtractionStatus.FAILED && status != ExtractionStatus.NO_DATA_EXTRACTED;
        }

        public ExtractedInvoiceData getBestData() {
            return enhancedData != null ? enhancedData : extractedData;
        }

        public String getSummary() {
            ExtractedInvoiceData data = getBestData();
            int fieldCount = data != null ? data.getExtractedFieldCount() : 0;
            return String.format("Status: %s | Fields: %d | Confidence: %.1f%% | Recommendation: %s",
                    status, fieldCount, overallConfidence * 100, recommendation);
        }
    }

    // Additional data classes for validation
    @lombok.Data
    @lombok.Builder
    public static class ValidationResult {
        private boolean isValid;
        private List<ValidationError> errors;
        private List<ValidationWarning> warnings;
        private Instant validatedAt;

        public boolean hasOnlyWarnings() {
            return isValid && (warnings != null && !warnings.isEmpty());
        }

        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class ValidationError {
        private String field;
        private String errorType;
        private String message;
        private String severity;
    }

    @lombok.Data
    @lombok.Builder
    public static class ValidationWarning {
        private String field;
        private String warningType;
        private String message;
        private String suggestedValue;
    }
}
