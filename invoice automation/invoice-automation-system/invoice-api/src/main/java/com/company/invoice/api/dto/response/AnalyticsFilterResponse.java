package com.company.invoice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for analytics filter options
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsFilterResponse {
    
    private DateRangeInfo dateRange;
    private List<FilterOption> statuses;
    private List<FilterOption> currencies;
    private List<FilterOption> vendors;
    private List<FilterOption> granularities;
    private List<FilterOption> metricTypes;
    private Map<String, Object> suggestions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRangeInfo {
        private LocalDate earliestDate;
        private LocalDate latestDate;
        private LocalDate suggestedStartDate;
        private LocalDate suggestedEndDate;
        private List<QuickRange> quickRanges;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickRange {
        private String label;
        private String value;
        private int days;
        private String description;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterOption {
        private String value;
        private String label;
        private String description;
        private Long count; // Number of items with this value
        private Boolean recommended; // Whether this is a recommended filter
        private Map<String, Object> metadata; // Additional metadata
    }
}
