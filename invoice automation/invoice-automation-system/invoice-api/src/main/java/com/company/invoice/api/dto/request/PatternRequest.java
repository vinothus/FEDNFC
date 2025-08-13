package com.company.invoice.api.dto.request;

import com.company.invoice.data.entity.InvoicePattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating invoice patterns
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for creating or updating invoice patterns")
public class PatternRequest {

    @NotBlank(message = "Pattern name is required")
    @Schema(description = "Name of the pattern", example = "Enhanced Invoice Number Pattern")
    private String patternName;

    @NotNull(message = "Pattern category is required")
    @Schema(description = "Category of the pattern", example = "INVOICE_NUMBER")
    private InvoicePattern.PatternCategory patternCategory;

    @NotBlank(message = "Pattern regex is required")
    @Schema(description = "Regular expression pattern", example = "(?i)(?:invoice\\s*(?:number|no|#)?[:\\s]*)?([A-Z0-9-]+)")
    private String patternRegex;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 100, message = "Priority must be at most 100")
    @Schema(description = "Pattern priority (1-100, higher = more priority)", example = "90")
    private Integer patternPriority;

    @Min(value = 0, message = "Confidence weight must be at least 0")
    @Max(value = 100, message = "Confidence weight must be at most 100")
    @Schema(description = "Confidence weight for scoring (0-100)", example = "85")
    private Integer confidenceWeight;

    @Schema(description = "Whether the pattern is active", example = "true")
    @Builder.Default
    private Boolean isActive = true;

    @Schema(description = "Pattern flags for regex compilation", example = "CASE_INSENSITIVE")
    private String patternFlags;

    @Schema(description = "Date format for date patterns", example = "yyyy-MM-dd")
    private String dateFormat;

    @Schema(description = "Description of what this pattern matches", example = "Matches invoice numbers in various formats")
    private String patternDescription;

    @Schema(description = "Additional notes about the pattern", example = "Works well with European invoice formats")
    private String notes;
}
