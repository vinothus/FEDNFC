package com.company.invoice.api.dto.response;

import com.company.invoice.data.entity.InvoicePattern;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for invoice pattern data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Invoice pattern response")
public class PatternResponse {

    @Schema(description = "Pattern ID", example = "1")
    private Long id;

    @Schema(description = "Pattern name", example = "Enhanced Invoice Number Pattern")
    private String patternName;

    @Schema(description = "Pattern category", example = "INVOICE_NUMBER")
    private String patternCategory;

    @Schema(description = "Regular expression pattern")
    private String patternRegex;

    @Schema(description = "Pattern priority", example = "90")
    private Integer patternPriority;

    @Schema(description = "Confidence weight", example = "85")
    private Integer confidenceWeight;

    @Schema(description = "Whether pattern is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Pattern flags", example = "CASE_INSENSITIVE")
    private String patternFlags;

    @Schema(description = "Date format for date patterns", example = "yyyy-MM-dd")
    private String dateFormat;

    @Schema(description = "Pattern description")
    private String patternDescription;

    @Schema(description = "Additional notes")
    private String notes;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Usage statistics")
    private PatternUsageStats usageStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Pattern usage statistics")
    public static class PatternUsageStats {
        @Schema(description = "Number of times pattern was used", example = "127")
        private Long usageCount;

        @Schema(description = "Success rate percentage", example = "94.5")
        private Double successRate;

        @Schema(description = "Last used timestamp")
        private LocalDateTime lastUsed;

        @Schema(description = "Average confidence when used", example = "89.3")
        private Double avgConfidence;
    }

    /**
     * Convert InvoicePattern entity to PatternResponse DTO
     */
    public static PatternResponse fromEntity(InvoicePattern pattern) {
        return PatternResponse.builder()
            .id(pattern.getId())
            .patternName(pattern.getPatternName())
            .patternCategory(pattern.getPatternCategory().name())
            .patternRegex(pattern.getPatternRegex())
            .patternPriority(pattern.getPatternPriority() != null ? pattern.getPatternPriority().intValue() : null)
            .confidenceWeight(pattern.getConfidenceWeight() != null ? pattern.getConfidenceWeight().intValue() : null)
            .isActive(pattern.getIsActive())
            .patternFlags(pattern.getPatternFlags().toString())
            .dateFormat(pattern.getDateFormat())
            .patternDescription(pattern.getPatternDescription())
            .notes(pattern.getNotes())
            .createdAt(pattern.getCreatedAt())
            .updatedAt(pattern.getUpdatedAt())
            .build();
    }

    /**
     * Convert InvoicePattern entity to PatternResponse DTO with usage stats
     */
    public static PatternResponse fromEntityWithStats(InvoicePattern pattern, PatternUsageStats stats) {
        PatternResponse response = fromEntity(pattern);
        response.setUsageStats(stats);
        return response;
    }
}
