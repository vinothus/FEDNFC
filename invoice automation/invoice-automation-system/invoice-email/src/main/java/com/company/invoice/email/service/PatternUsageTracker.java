package com.company.invoice.email.service;

import com.company.invoice.data.entity.InvoicePattern;
import com.company.invoice.ocr.service.parsing.DatabasePatternLibrary;
import com.company.invoice.ocr.service.parsing.InvoiceDataExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for tracking pattern usage in invoice data extraction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatternUsageTracker {

    private final ObjectMapper objectMapper;

    /**
     * Create pattern usage summary from extraction results with field mapping
     */
    public PatternUsageData createPatternUsageData(Map<String, DatabasePatternLibrary.ExtractionResult> fieldResults) {
        if (fieldResults == null || fieldResults.isEmpty()) {
            return PatternUsageData.empty();
        }

        // Extract pattern IDs and create summary
        List<Long> patternIds = fieldResults.values().stream()
                .filter(r -> r.patternUsed() != null && !r.patternUsed().trim().isEmpty())
                .map(r -> extractPatternIdFromName(r.patternUsed()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Create human-readable summary
        String summary = createPatternSummary(fieldResults);

        // Create detailed confidence JSON
        String confidenceDetails = createConfidenceDetailsJson(fieldResults);

        return PatternUsageData.builder()
                .usedPatternIds(patternIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .patternMatchSummary(summary)
                .extractionConfidenceDetails(confidenceDetails)
                .build();
    }

    /**
     * Create pattern usage summary from field extractions (overloaded method)
     */
    public PatternUsageData createPatternUsageData(List<InvoiceDataExtractor.FieldExtraction> fieldExtractions) {
        if (fieldExtractions == null || fieldExtractions.isEmpty()) {
            return PatternUsageData.empty();
        }

        // Convert FieldExtraction to DatabasePatternLibrary.ExtractionResult format for processing
        Map<String, DatabasePatternLibrary.ExtractionResult> fieldResults = new HashMap<>();
        
        for (InvoiceDataExtractor.FieldExtraction extraction : fieldExtractions) {
            // Create a mock ExtractionResult from FieldExtraction
            DatabasePatternLibrary.ExtractionResult result = new DatabasePatternLibrary.ExtractionResult(
                extraction.getValue(),
                extraction.getConfidence(),
                extraction.getPattern() != null ? extraction.getPattern() : "UNKNOWN_PATTERN"
            );
            fieldResults.put(extraction.getFieldName(), result);
        }

        // Use the existing logic
        return createPatternUsageData(fieldResults);
    }

    /**
     * Extract pattern ID from pattern name (assuming pattern names contain ID or can be mapped)
     */
    private Long extractPatternIdFromName(String patternName) {
        try {
            // For now, we'll need to implement a mapping strategy
            // This could be enhanced to query the database for pattern ID by name
            // For demonstration, we'll use a hash-based approach
            return (long) Math.abs(patternName.hashCode() % 10000);
        } catch (Exception e) {
            log.warn("Could not extract pattern ID from name: {}", patternName);
            return null;
        }
    }

    /**
     * Create human-readable pattern match summary
     */
    private String createPatternSummary(Map<String, DatabasePatternLibrary.ExtractionResult> fieldResults) {
        StringBuilder summary = new StringBuilder();
        
        for (Map.Entry<String, DatabasePatternLibrary.ExtractionResult> entry : fieldResults.entrySet()) {
            String fieldName = entry.getKey();
            DatabasePatternLibrary.ExtractionResult result = entry.getValue();
            
            if (result != null && result.value() != null) {
                summary.append(String.format("%s: '%s' (pattern: %s, confidence: %.1f%%) | ",
                        fieldName, result.value(), result.patternUsed(), 
                        result.confidence() * 100));
            }
        }
        
        return summary.length() > 0 ? summary.substring(0, summary.length() - 3) : "No patterns matched";
    }

    /**
     * Create detailed confidence JSON
     */
    private String createConfidenceDetailsJson(Map<String, DatabasePatternLibrary.ExtractionResult> fieldResults) {
        try {
            Map<String, Object> details = new HashMap<>();
            
            for (Map.Entry<String, DatabasePatternLibrary.ExtractionResult> entry : fieldResults.entrySet()) {
                String fieldName = entry.getKey();
                DatabasePatternLibrary.ExtractionResult result = entry.getValue();
                
                Map<String, Object> fieldDetails = new HashMap<>();
                fieldDetails.put("value", result.value());
                fieldDetails.put("confidence", result.confidence());
                fieldDetails.put("pattern_used", result.patternUsed());
                
                details.put(fieldName, fieldDetails);
            }
            
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.error("Failed to create confidence details JSON", e);
            return "{}";
        }
    }

    /**
     * Data class for pattern usage information
     */
    @lombok.Data
    @lombok.Builder
    public static class PatternUsageData {
        private String usedPatternIds;
        private String patternMatchSummary;
        private String extractionConfidenceDetails;

        public static PatternUsageData empty() {
            return PatternUsageData.builder()
                    .usedPatternIds("")
                    .patternMatchSummary("No patterns matched")
                    .extractionConfidenceDetails("{}")
                    .build();
        }
    }
}
